package io.nexflow.engine.core.unit.builtin;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import io.nexflow.engine.core.builtin.ConditionStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConditionStep: operators, branches TRUE/FALSE, missing config.
 */
@DisplayName("ConditionStep unit tests")
class ConditionStepTest {

    private ConditionStep step;

    @BeforeEach
    void setUp() {
        step = new ConditionStep();
    }

    @Test
    @DisplayName("getType returns condition")
    void getType() {
        assertEquals("condition", step.getType());
    }

    @Nested
    @DisplayName("numeric comparison")
    class NumericComparison {

        @Test
        @DisplayName("inputs.amount > 1000 branches TRUE when true")
        void gtBranchesTrue() {
            StepExecutionContext ctx = new StepExecutionContext("e1", "wf", "t1", 0,
                    Map.of("amount", 1500),
                    Map.of("field", "amount", "operator", ">", "value", 1000));
            StepResult r = step.execute(ctx);
            assertEquals(StepResult.Status.BRANCH, r.getStatus());
            assertEquals("TRUE", r.getBranchName());
        }

        @Test
        @DisplayName("inputs.amount > 1000 branches FALSE when false")
        void gtBranchesFalse() {
            StepExecutionContext ctx = new StepExecutionContext("e1", "wf", "t1", 0,
                    Map.of("amount", 500),
                    Map.of("field", "amount", "operator", ">", "value", 1000));
            StepResult r = step.execute(ctx);
            assertEquals(StepResult.Status.BRANCH, r.getStatus());
            assertEquals("FALSE", r.getBranchName());
        }

        @Test
        @DisplayName("operator eq with equal numbers branches TRUE")
        void eqBranchesTrue() {
            StepExecutionContext ctx = new StepExecutionContext("e1", "wf", "t1", 0,
                    Map.of("x", 10),
                    Map.of("field", "x", "operator", "==", "value", 10));
            StepResult r = step.execute(ctx);
            assertEquals("TRUE", r.getBranchName());
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("missing field returns FAILURE")
        void missingFieldReturnsFailure() {
            StepExecutionContext ctx = new StepExecutionContext("e1", "wf", "t1", 0,
                    Map.of(),
                    Map.of("operator", "==", "value", 1));
            StepResult r = step.execute(ctx);
            assertEquals(StepResult.Status.FAILURE, r.getStatus());
        }

        @Test
        @DisplayName("missing operator returns FAILURE")
        void missingOperatorReturnsFailure() {
            StepExecutionContext ctx = new StepExecutionContext("e1", "wf", "t1", 0,
                    Map.of("x", 1),
                    Map.of("field", "x", "value", 1));
            StepResult r = step.execute(ctx);
            assertEquals(StepResult.Status.FAILURE, r.getStatus());
        }
    }
}
