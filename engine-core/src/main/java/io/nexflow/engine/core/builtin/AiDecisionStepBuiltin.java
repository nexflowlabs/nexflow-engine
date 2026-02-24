package io.nexflow.engine.core.builtin;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import com.nexflow.sdk.core.WorkflowStep;
import io.nexflow.engine.core.step.AiDecisionEvaluator;

import java.util.Map;

import static io.nexflow.engine.core.builtin.StepConstants.OUTCOME_KEY;

/**
 * Built-in step: AI decision. Config: endpoint (or promptKey), confidenceThreshold. Branches: HIGH, LOW.
 */
public final class AiDecisionStepBuiltin implements WorkflowStep {

    private static final String TYPE_AI_DECISION = "aiDecision";
    private static final String CONFIG_PROMPT_KEY = "promptKey";
    private static final String CONFIG_ENDPOINT = "endpoint";
    private static final String CONFIG_CONFIDENCE_THRESHOLD = "confidenceThreshold";
    private static final String BRANCH_HIGH = "HIGH";
    private static final String BRANCH_LOW = "LOW";
    private static final String ERROR_EVALUATOR_NOT_CONFIGURED = "AiDecisionEvaluator not configured";
    private static final String ERROR_REQUIRES_ENDPOINT_OR_PROMPT = "aiDecision requires endpoint or promptKey";
    private static final String ERROR_THRESHOLD_RANGE = "confidenceThreshold must be in [0, 1]";

    private static final double DEFAULT_THRESHOLD = 0.7;

    private final AiDecisionEvaluator evaluator;

    public AiDecisionStepBuiltin(AiDecisionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public String getType() {
        return TYPE_AI_DECISION;
    }

    @Override
    public StepResult execute(StepExecutionContext context) {
        if (evaluator == null) {
            return StepResult.failure(ERROR_EVALUATOR_NOT_CONFIGURED);
        }
        Map<String, Object> config = context.getStepConfig();
        String promptKey = (String) config.getOrDefault(CONFIG_PROMPT_KEY, config.get(CONFIG_ENDPOINT));
        if (promptKey == null || promptKey.isBlank()) {
            return StepResult.failure(ERROR_REQUIRES_ENDPOINT_OR_PROMPT);
        }
        Object t = config.get(CONFIG_CONFIDENCE_THRESHOLD);
        double threshold = t instanceof Number ? ((Number) t).doubleValue() : DEFAULT_THRESHOLD;
        if (threshold < 0 || threshold > 1) {
            return StepResult.failure(ERROR_THRESHOLD_RANGE);
        }
        double confidence = evaluator.evaluateConfidence(promptKey, context.getInputs());
        String branch = confidence >= threshold ? BRANCH_HIGH : BRANCH_LOW;
        return StepResult.branch(branch, Map.of(OUTCOME_KEY, branch));
    }
}
