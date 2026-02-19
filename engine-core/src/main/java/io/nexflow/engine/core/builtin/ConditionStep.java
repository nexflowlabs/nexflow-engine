package io.nexflow.engine.core.builtin;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import com.nexflow.sdk.core.WorkflowStep;

import java.util.Map;
import java.util.Objects;

import static io.nexflow.engine.core.builtin.StepConstants.OUTCOME_KEY;

/**
 * Built-in step: simple comparison. Config: field, operator, value. Branches: TRUE, FALSE.
 */
public final class ConditionStep implements WorkflowStep {

    private static final String TYPE_CONDITION = "condition";
    private static final String CONFIG_FIELD = "field";
    private static final String CONFIG_OPERATOR = "operator";
    private static final String CONFIG_VALUE = "value";
    private static final String BRANCH_TRUE = "TRUE";
    private static final String BRANCH_FALSE = "FALSE";
    private static final String ERROR_REQUIRES_FIELD_OPERATOR = "condition requires field and operator";

    private static final String OP_EQ = "==";
    private static final String OP_EQUALS = "=";
    private static final String OP_NE = "!=";
    private static final String OP_GT = ">";
    private static final String OP_GTE = ">=";
    private static final String OP_LT = "<";
    private static final String OP_LTE = "<=";
    private static final String OP_GT_ALT = "gt";
    private static final String OP_GTE_ALT = "gte";
    private static final String OP_LT_ALT = "lt";
    private static final String OP_LTE_ALT = "lte";
    private static final String OP_EQ_ALT = "eq";
    private static final String OP_NE_ALT = "neq";

    @Override
    public String getType() {
        return TYPE_CONDITION;
    }

    @Override
    public StepResult execute(StepExecutionContext context) {
        Map<String, Object> config = context.getStepConfig();
        String field = (String) config.get(CONFIG_FIELD);
        String operator = (String) config.get(CONFIG_OPERATOR);
        Object expected = config.get(CONFIG_VALUE);
        if (field == null || operator == null) {
            return StepResult.failure(ERROR_REQUIRES_FIELD_OPERATOR);
        }
        Map<String, Object> inputs = context.getInputs();
        Object actual = inputs.get(field);
        boolean result = evaluate(operator, actual, expected);
        String branch = result ? BRANCH_TRUE : BRANCH_FALSE;
        return StepResult.branch(branch, Map.of(OUTCOME_KEY, branch));
    }

    private static boolean evaluate(String operator, Object actual, Object expected) {
        if (actual == null && expected == null) return OP_EQ.equals(operator) || OP_EQUALS.equals(operator);
        if (actual == null || expected == null) return OP_NE.equals(operator);
        if (actual instanceof Number && expected instanceof Number) {
            double a = ((Number) actual).doubleValue();
            double b = ((Number) expected).doubleValue();
            return switch (operator) {
                case OP_GT, OP_GT_ALT -> a > b;
                case OP_GTE, OP_GTE_ALT -> a >= b;
                case OP_LT, OP_LT_ALT -> a < b;
                case OP_LTE, OP_LTE_ALT -> a <= b;
                case OP_EQ, OP_EQUALS, OP_EQ_ALT -> a == b;
                case OP_NE, OP_NE_ALT -> a != b;
                default -> false;
            };
        }
        return switch (operator) {
            case OP_EQ, OP_EQUALS, OP_EQ_ALT -> Objects.equals(actual, expected);
            case OP_NE, OP_NE_ALT -> !Objects.equals(actual, expected);
            default -> false;
        };
    }
}
