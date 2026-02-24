package io.nexflow.engine.core.builtin;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import io.nexflow.engine.core.script.ScriptExecutionClient;
import io.nexflow.engine.core.script.ScriptExecutionResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScriptStepTest {

    private static final boolean GRAALVM_AVAILABLE = checkGraalVmAvailable();

    private static boolean checkGraalVmAvailable() {
        try {
            org.graalvm.polyglot.Context ctx = org.graalvm.polyglot.Context.newBuilder("js").allowAllAccess(false).build();
            ctx.close();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private ScriptStep inProcessStep;
    private ScriptStep isolatedStep;

    @Mock
    private ScriptExecutionClient isolatedClient;

    @BeforeEach
    void setUp() {
        inProcessStep = new ScriptStep();
        isolatedStep = new ScriptStep(isolatedClient);
    }

    @Test
    void getType() {
        assertEquals("script", inProcessStep.getType());
    }

    @Test
    void inProcess_success() {
        Assumptions.assumeTrue(GRAALVM_AVAILABLE, "GraalVM JS not available on this JVM");
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of("amount", 100),
                Map.of("code", "success({ result: inputs.amount * 2 });")
        );
        StepResult result = inProcessStep.execute(context);
        assertEquals(StepResult.Status.SUCCESS, result.getStatus());
        assertEquals(200, ((Number) result.getOutputs().get("result")).intValue());
    }

    @Test
    void inProcess_failure() {
        Assumptions.assumeTrue(GRAALVM_AVAILABLE, "GraalVM JS not available on this JVM");
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of(),
                Map.of("code", "failure();")
        );
        StepResult result = inProcessStep.execute(context);
        assertEquals(StepResult.Status.FAILURE, result.getStatus());
    }

    @Test
    void inProcess_branch() {
        Assumptions.assumeTrue(GRAALVM_AVAILABLE, "GraalVM JS not available on this JVM");
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of("amount", 1500),
                Map.of("code", "if (inputs.amount > 1000) branch('HIGH'); else branch('LOW');")
        );
        StepResult result = inProcessStep.execute(context);
        assertEquals(StepResult.Status.BRANCH, result.getStatus());
        assertEquals("HIGH", result.getBranchName());
    }

    @Test
    void inProcess_noHelperCall_returnsFailure() {
        Assumptions.assumeTrue(GRAALVM_AVAILABLE, "GraalVM JS not available on this JVM");
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of(),
                Map.of("code", "var x = 1 + 1;")
        );
        StepResult result = inProcessStep.execute(context);
        assertEquals(StepResult.Status.FAILURE, result.getStatus());
    }

    @Test
    void isolatedMode_delegatesToClient() {
        when(isolatedClient.execute(eq("javascript"), contains("branch"), any(), any(), eq(200L)))
                .thenReturn(ScriptExecutionResult.branch("MANUAL"));
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of("x", 1),
                Map.of("mode", "ISOLATED", "code", "branch('MANUAL');", "language", "javascript")
        );
        StepResult result = isolatedStep.execute(context);
        assertEquals(StepResult.Status.BRANCH, result.getStatus());
        assertEquals("MANUAL", result.getBranchName());
    }

    @Test
    void isolatedMode_clientReturnsSuccess() {
        when(isolatedClient.execute(any(), any(), any(), any(), anyLong()))
                .thenReturn(ScriptExecutionResult.success(Map.of("out", "value")));
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of(),
                Map.of("mode", "ISOLATED", "code", "success({});")
        );
        StepResult result = isolatedStep.execute(context);
        assertEquals(StepResult.Status.SUCCESS, result.getStatus());
        assertEquals("value", result.getOutputs().get("out"));
    }

    @Test
    void isolatedMode_noClient_fallsBackToInProcess() {
        Assumptions.assumeTrue(GRAALVM_AVAILABLE, "GraalVM JS not available on this JVM");
        ScriptStep stepNoClient = new ScriptStep(null);
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of(),
                Map.of("mode", "ISOLATED", "code", "success({});")
        );
        StepResult result = stepNoClient.execute(context);
        assertEquals(StepResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void missingCode_returnsFailure() {
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of(),
                Map.of()
        );
        StepResult result = inProcessStep.execute(context);
        assertEquals(StepResult.Status.FAILURE, result.getStatus());
    }

    @Test
    void timeoutMs_fromConfig() {
        when(isolatedClient.execute(any(), any(), any(), any(), eq(500L)))
                .thenReturn(ScriptExecutionResult.success(Map.of()));
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of(),
                Map.of("mode", "ISOLATED", "code", "success({});", "timeoutMs", 500)
        );
        isolatedStep.execute(context);
        // verified via mock: timeout 500 was passed
    }

    @Test
    void branchEnforcement_blankBranch_returnsFailure() {
        Assumptions.assumeTrue(GRAALVM_AVAILABLE, "GraalVM JS not available on this JVM");
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of(),
                Map.of("code", "branch('');")
        );
        StepResult result = inProcessStep.execute(context);
        assertEquals(StepResult.Status.FAILURE, result.getStatus());
    }

    @Test
    void timeout_exceedsLimit_returnsFailure() {
        Assumptions.assumeTrue(GRAALVM_AVAILABLE, "GraalVM JS not available on this JVM");
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of(),
                Map.of("code", "for (var i = 0; i < 1e8; i++) {} success({});", "timeoutMs", 1)
        );
        StepResult result = inProcessStep.execute(context);
        assertEquals(StepResult.Status.FAILURE, result.getStatus());
        assertTrue(result.getErrorMessage() == null || result.getErrorMessage().contains("timeout"));
    }
}
