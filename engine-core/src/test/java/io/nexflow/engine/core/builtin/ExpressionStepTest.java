package io.nexflow.engine.core.builtin;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionStepTest {

    private ExpressionStep step;

    @BeforeEach
    void setUp() {
        step = new ExpressionStep();
    }

    @Test
    void getType() {
        assertEquals("expression", step.getType());
    }

    @Test
    void expressionTrue_branchesTrue() {
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of("amount", 1500, "country", "US"),
                Map.of("expression", "inputs['amount'] > 1000 && inputs['country'] == 'US'")
        );
        StepResult result = step.execute(context);
        assertEquals(StepResult.Status.BRANCH, result.getStatus());
        assertEquals("true", result.getBranchName());
    }

    @Test
    void expressionFalse_branchesFalse() {
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of("amount", 500, "country", "US"),
                Map.of("expression", "inputs['amount'] > 1000 && inputs['country'] == 'US'")
        );
        StepResult result = step.execute(context);
        assertEquals(StepResult.Status.BRANCH, result.getStatus());
        assertEquals("false", result.getBranchName());
    }

    @Test
    void missingExpression_returnsFailure() {
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of(),
                Map.of()
        );
        StepResult result = step.execute(context);
        assertEquals(StepResult.Status.FAILURE, result.getStatus());
    }

    @Test
    void invalidExpression_returnsFailure() {
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of(),
                Map.of("expression", "inputs['xxx'] && invalid syntax {{")
        );
        StepResult result = step.execute(context);
        assertEquals(StepResult.Status.FAILURE, result.getStatus());
    }

    @Test
    void stepConfigAccessible() {
        StepExecutionContext context = new StepExecutionContext(
                "ex1", "wf", "t1", 0,
                Map.of("x", 1),
                Map.of("expression", "stepConfig['threshold'] != null", "threshold", 10)
        );
        StepResult result = step.execute(context);
        assertEquals(StepResult.Status.BRANCH, result.getStatus());
        assertEquals("true", result.getBranchName());
    }
}
