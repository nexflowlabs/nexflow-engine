package io.nexflow.engine.core.unit;

import com.nexflow.sdk.core.StepResult;
import com.nexflow.sdk.core.WorkflowStep;
import io.nexflow.engine.core.definition.StepDefinition;
import io.nexflow.engine.core.definition.WorkflowDefinition;
import io.nexflow.engine.core.registry.StepRegistry;
import io.nexflow.engine.core.runtime.ExecutionContext;
import io.nexflow.engine.core.runtime.RegistryDrivenRuntime;
import io.nexflow.engine.core.runtime.StepOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RegistryDrivenRuntime: SUCCESS/BRANCH/FAILURE/RETRY outcome and retry behavior.
 */
@DisplayName("RegistryDrivenRuntime unit tests")
class RegistryDrivenRuntimeTest {

    private static final int ID_A = 1;
    private static final int ID_B = 2;
    private static final int ID_C = 3;
    private static final Integer STEP_A = 1;
    private static final Integer STEP_B = 2;
    private static final Integer STEP_C = 3;

    private WorkflowDefinition workflow;
    private RegistryDrivenRuntime runtime;

    @BeforeEach
    void setUp() {
        StepDefinition stepA = new StepDefinition(ID_A, "stepA", "stub", Map.of(), Map.of("success", STEP_B));
        Map<String, Integer> branchB = new HashMap<>();
        branchB.put("success", null);
        StepDefinition stepB = new StepDefinition(ID_B, "stepB", "stub", Map.of(), branchB);

        workflow = new WorkflowDefinition();
        workflow.setSteps(List.of(stepA, stepB));
    }

    @Nested
    @DisplayName("SUCCESS outcome (uses branch 'success')")
    class SuccessOutcome {

        @Test
        @DisplayName("SUCCESS with branch 'success' -> step id returns CONTINUE to next")
        void successWithBranchTargetReturnsContinue() {
            WorkflowStep stub = ctx -> StepResult.success(Map.of("outcome", "success", "out", 1));
            runtime = new RegistryDrivenRuntime(new StepRegistry(Map.of("stub", stub)));
            ExecutionContext ctx = new ExecutionContext("ex1", new java.util.HashMap<>(Map.of("in", 0)));

            StepOutcome outcome = runtime.runStep(workflow, STEP_A, ctx, "wf", "t1", 0);

            assertEquals(StepOutcome.Type.CONTINUE, outcome.getType());
            assertEquals(STEP_B, outcome.getNextStepId());
            assertEquals(1, outcome.getContextUpdates().get("out"));
        }

        @Test
        @DisplayName("SUCCESS with branch 'success' -> null returns COMPLETE")
        void successWithBranchTargetNullReturnsComplete() {
            WorkflowStep stub = ctx -> StepResult.success(Map.of("outcome", "success", "done", true));
            runtime = new RegistryDrivenRuntime(new StepRegistry(Map.of("stub", stub)));
            ExecutionContext ctx = new ExecutionContext("ex1", new java.util.HashMap<>());

            StepOutcome outcome = runtime.runStep(workflow, STEP_B, ctx, "wf", "t1", 0);

            assertEquals(StepOutcome.Type.COMPLETE, outcome.getType());
            assertNull(outcome.getNextStepId());
            assertTrue((Boolean) outcome.getContextUpdates().get("done"));
        }

        @Test
        @DisplayName("SUCCESS when step has no 'success' branch returns FAIL with message")
        void successWhenNoSuccessBranchReturnsFail() {
            StepDefinition stepNoSuccess = new StepDefinition(ID_A, "stepA", "stub", Map.of(), Map.of("other", STEP_B));
            workflow.setSteps(List.of(stepNoSuccess, workflow.getSteps().get(1)));
            WorkflowStep stub = ctx -> StepResult.success(Map.of("outcome", "success"));
            runtime = new RegistryDrivenRuntime(new StepRegistry(Map.of("stub", stub)));
            ExecutionContext ctx = new ExecutionContext("ex1", new java.util.HashMap<>());

            StepOutcome outcome = runtime.runStep(workflow, STEP_A, ctx, "wf", "t1", 0);

            assertEquals(StepOutcome.Type.FAIL, outcome.getType());
            assertNotNull(outcome.getErrorMessage());
            assertTrue(outcome.getErrorMessage().contains("Unknown branch"));
        }
    }

    @Nested
    @DisplayName("BRANCH outcome")
    class BranchOutcome {

        @Test
        @DisplayName("BRANCH resolves next from branches map")
        void branchResolvesNextFromBranchesMap() {
            StepDefinition stepA = new StepDefinition(ID_A, "stepA", "stub", Map.of(), Map.of("HIGH", ID_B, "LOW", ID_C));
            StepDefinition stepB = workflow.getSteps().get(1);
            StepDefinition stepC = new StepDefinition(ID_C, "stepC", "stub", Map.of(), Map.of());
            workflow.setSteps(List.of(stepA, stepB, stepC));

            WorkflowStep stub = ctx -> StepResult.branch("HIGH", Map.of("outcome", "HIGH"));
            runtime = new RegistryDrivenRuntime(new StepRegistry(Map.of("stub", stub)));
            ExecutionContext ctx = new ExecutionContext("ex1", new java.util.HashMap<>());

            StepOutcome outcome = runtime.runStep(workflow, STEP_A, ctx, "wf", "t1", 0);

            assertEquals(StepOutcome.Type.CONTINUE, outcome.getType());
            assertEquals(STEP_B, outcome.getNextStepId());
        }

        @Test
        @DisplayName("BRANCH with unknown name returns FAIL with message")
        void branchUnknownReturnsFail() {
            StepDefinition stepA = new StepDefinition(ID_A, "stepA", "stub", Map.of(), Map.of("HIGH", ID_B));
            workflow.setSteps(List.of(stepA, workflow.getSteps().get(1)));
            WorkflowStep stub = ctx -> StepResult.branch("UNKNOWN", Map.of("outcome", "UNKNOWN"));
            runtime = new RegistryDrivenRuntime(new StepRegistry(Map.of("stub", stub)));
            ExecutionContext ctx = new ExecutionContext("ex1", new java.util.HashMap<>());

            StepOutcome outcome = runtime.runStep(workflow, STEP_A, ctx, "wf", "t1", 0);

            assertEquals(StepOutcome.Type.FAIL, outcome.getType());
            assertNotNull(outcome.getErrorMessage());
            assertTrue(outcome.getErrorMessage().contains("Unknown branch"));
        }

        @Test
        @DisplayName("BRANCH with null target returns COMPLETE")
        void branchWithNullTargetReturnsComplete() {
            Map<String, Integer> branchDone = new HashMap<>();
            branchDone.put("done", null);
            StepDefinition stepA = new StepDefinition(ID_A, "stepA", "stub", Map.of(), branchDone);
            workflow.setSteps(List.of(stepA, workflow.getSteps().get(1)));
            WorkflowStep stub = ctx -> StepResult.branch("done", Map.of("outcome", "done"));
            runtime = new RegistryDrivenRuntime(new StepRegistry(Map.of("stub", stub)));
            ExecutionContext ctx = new ExecutionContext("ex1", new java.util.HashMap<>());

            StepOutcome outcome = runtime.runStep(workflow, STEP_A, ctx, "wf", "t1", 0);

            assertEquals(StepOutcome.Type.COMPLETE, outcome.getType());
            assertNull(outcome.getNextStepId());
        }
    }

    @Nested
    @DisplayName("outcome required")
    class OutcomeRequired {

        @Test
        @DisplayName("SUCCESS without outcome in outputs returns FAIL with message")
        void successWithoutOutcomeReturnsFail() {
            WorkflowStep stub = ctx -> StepResult.success(Map.of("data", 1));
            runtime = new RegistryDrivenRuntime(new StepRegistry(Map.of("stub", stub)));
            ExecutionContext ctx = new ExecutionContext("ex1", new java.util.HashMap<>());

            StepOutcome outcome = runtime.runStep(workflow, STEP_A, ctx, "wf", "t1", 0);

            assertEquals(StepOutcome.Type.FAIL, outcome.getType());
            assertNotNull(outcome.getErrorMessage());
            assertTrue(outcome.getErrorMessage().contains("outcome is required"));
        }
    }

    @Nested
    @DisplayName("FAILURE outcome")
    class FailureOutcome {

        @Test
        @DisplayName("FAILURE returns StepOutcome fail")
        void failureReturnsFail() {
            WorkflowStep stub = ctx -> StepResult.failure("error");
            runtime = new RegistryDrivenRuntime(new StepRegistry(Map.of("stub", stub)));
            ExecutionContext ctx = new ExecutionContext("ex1", new java.util.HashMap<>());

            StepOutcome outcome = runtime.runStep(workflow, STEP_A, ctx, "wf", "t1", 0);

            assertEquals(StepOutcome.Type.FAIL, outcome.getType());
            assertNull(outcome.getNextStepId());
        }
    }

    @Nested
    @DisplayName("RETRY behavior")
    class RetryBehavior {

        @Test
        @DisplayName("RETRY returns CONTINUE to same step id so engine can retry")
        void retryReturnsContinueToSameStepId() {
            WorkflowStep stub = ctx -> StepResult.retry();
            runtime = new RegistryDrivenRuntime(new StepRegistry(Map.of("stub", stub)));
            ExecutionContext ctx = new ExecutionContext("ex1", new java.util.HashMap<>());

            StepOutcome outcome = runtime.runStep(workflow, STEP_A, ctx, "wf", "t1", 0);

            assertEquals(StepOutcome.Type.CONTINUE, outcome.getType());
            assertEquals(STEP_A, outcome.getNextStepId());
            assertTrue(outcome.getContextUpdates().isEmpty());
        }
    }

    @Nested
    @DisplayName("getStartStepId")
    class GetStartStepId {

        @Test
        @DisplayName("returns first step id when steps non-empty")
        void returnsFirstStepId() {
            assertEquals(Integer.valueOf(ID_A), RegistryDrivenRuntime.getStartStepId(workflow));
        }

        @Test
        @DisplayName("returns null when steps null or empty")
        void returnsNullWhenEmpty() {
            workflow.setSteps(null);
            assertNull(RegistryDrivenRuntime.getStartStepId(workflow));
            workflow.setSteps(List.of());
            assertNull(RegistryDrivenRuntime.getStartStepId(workflow));
        }
    }
}
