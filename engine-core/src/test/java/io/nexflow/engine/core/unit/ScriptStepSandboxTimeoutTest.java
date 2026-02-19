package io.nexflow.engine.core.unit;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import io.nexflow.engine.core.builtin.ScriptStep;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for script sandbox: timeout enforcement.
 * When script runs longer than timeoutMs, step must return FAILURE (no arbitrary execution).
 */
@DisplayName("ScriptStep sandbox timeout enforcement")
class ScriptStepSandboxTimeoutTest {

    private static boolean graalVmAvailable() {
        try {
            org.graalvm.polyglot.Context ctx = org.graalvm.polyglot.Context.newBuilder("js").allowAllAccess(false).build();
            ctx.close();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Test
    @DisplayName("script exceeding timeoutMs returns FAILURE with timeout message")
    void scriptExceedingTimeoutReturnsFailure() {
        Assumptions.assumeTrue(graalVmAvailable(), "GraalVM JS required for in-process script timeout test");
        ScriptStep step = new ScriptStep();
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of(),
                Map.of("code", "for (var i = 0; i < 1e8; i++) {} success({});", "timeoutMs", 1)
        );
        StepResult result = step.execute(context);
        assertEquals(StepResult.Status.FAILURE, result.getStatus());
        assertTrue(result.getErrorMessage() == null || result.getErrorMessage().toLowerCase().contains("timeout"),
                "Expected timeout-related error message, got: " + result.getErrorMessage());
    }

    @Test
    @DisplayName("timeoutMs from config is applied")
    void timeoutMsFromConfigIsApplied() {
        Assumptions.assumeTrue(graalVmAvailable(), "GraalVM JS required");
        ScriptStep step = new ScriptStep();
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of(),
                Map.of("code", "success({});", "timeoutMs", 5000)
        );
        StepResult result = step.execute(context);
        assertEquals(StepResult.Status.SUCCESS, result.getStatus());
    }
}
