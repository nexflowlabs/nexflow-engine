package io.nexflow.engine.core.builtin;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import com.nexflow.sdk.core.WorkflowStep;
import org.springframework.expression.ExpressionException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.nexflow.engine.core.builtin.StepConstants.OUTCOME_KEY;

/**
 * Built-in step: safe boolean expression evaluation (SpEL).
 * Config: expression (e.g. "inputs['amount'] > 1000 && inputs['country'] == 'US'").
 * Use bracket notation for map keys: inputs['key'], stepConfig['key']. Branches: "true", "false".
 * Only inputs and stepConfig are exposed; no reflection, system, file, or class loading.
 */
public final class ExpressionStep implements WorkflowStep {

    private static final String TYPE_EXPRESSION = "expression";
    private static final String CONFIG_EXPRESSION = "expression";
    private static final String BRANCH_TRUE = "true";
    private static final String BRANCH_FALSE = "false";
    private static final String ERROR_REQUIRES_EXPRESSION = "expression step requires config.expression";
    private static final String LOG_PREFIX = "expression step: ";

    private static final Logger LOG = Logger.getLogger(ExpressionStep.class.getName());

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    @Override
    public String getType() {
        return TYPE_EXPRESSION;
    }

    @Override
    public StepResult execute(StepExecutionContext context) {
        Map<String, Object> config = context.getStepConfig();
        String expression = config != null ? (String) config.get(CONFIG_EXPRESSION) : null;
        if (expression == null || expression.isBlank()) {
            LOG.log(Level.WARNING, LOG_PREFIX + "missing or empty 'expression' in config");
            return StepResult.failure(ERROR_REQUIRES_EXPRESSION);
        }

        Boolean result = evaluate(expression, context.getInputs(), context.getStepConfig());
        if (result == null) {
            return StepResult.failure();
        }
        String branch = Boolean.TRUE.equals(result) ? BRANCH_TRUE : BRANCH_FALSE;
        return StepResult.branch(branch, Map.of(OUTCOME_KEY, branch));
    }

    /**
     * Evaluate boolean expression with a restricted context: only inputs and stepConfig.
     * No Java reflection, system properties, file access, or class loading.
     * Root exposes getInputs() and getStepConfig(); use bracket notation for map keys, e.g. inputs['amount'] > 1000.
     */
    static Boolean evaluate(String expression, Map<String, Object> inputs, Map<String, Object> stepConfig) {
        Root root = new Root(inputs != null ? inputs : Map.of(), stepConfig != null ? stepConfig : Map.of());
        StandardEvaluationContext evalContext = new StandardEvaluationContext(root);
        evalContext.addPropertyAccessor(new MapAccessor());
        try {
            Object value = PARSER.parseExpression(expression).getValue(evalContext);
            if (value == null) {
                return false;
            }
            if (value instanceof Boolean b) {
                return b;
            }
            if (value instanceof Number n) {
                return n.intValue() != 0;
            }
            return Boolean.parseBoolean(String.valueOf(value));
        } catch (ExpressionException e) {
            LOG.log(Level.WARNING, LOG_PREFIX + "evaluation failed: " + e.getMessage());
            return null;
        }
    }

    /** Read-only root for SpEL: only inputs and stepConfig exposed. */
    @Getter
    @RequiredArgsConstructor
    public static final class Root {
        private final Map<String, Object> inputs;
        private final Map<String, Object> stepConfig;
    }
}
