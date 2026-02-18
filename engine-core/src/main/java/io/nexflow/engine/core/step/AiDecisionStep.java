package io.nexflow.engine.core.step;

import io.nexflow.engine.core.definition.StepDefinition;
import io.nexflow.engine.core.runtime.ExecutionContext;
import io.nexflow.engine.core.runtime.TransitionResult;

/**
 * Built-in step: AI decision. Calls evaluator for confidence and branches on threshold.
 * Validates branch safety (onHighConfidence/onLowConfidence must be non-null).
 */
public final class AiDecisionStep {

    private static final double DEFAULT_THRESHOLD = 0.8;

    private AiDecisionStep() {}

    public static TransitionResult execute(
            StepDefinition step,
            ExecutionContext context,
            AiDecisionEvaluator evaluator
    ) {
        if (evaluator == null) {
            throw new IllegalStateException("AiDecisionEvaluator not configured");
        }
        String promptKey = step.getPromptKey();
        if (promptKey == null || promptKey.isBlank()) {
            throw new IllegalArgumentException("aiDecision step requires promptKey");
        }
        double threshold = step.getConfidenceThreshold() != null
                ? step.getConfidenceThreshold()
                : DEFAULT_THRESHOLD;
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("confidenceThreshold must be in [0, 1]");
        }
        String onHigh = step.getOnHighConfidence();
        String onLow = step.getOnLowConfidence();
        if (onHigh == null || onHigh.isBlank() || onLow == null || onLow.isBlank()) {
            throw new IllegalArgumentException("aiDecision step requires onHighConfidence and onLowConfidence");
        }

        double confidence = evaluator.evaluateConfidence(promptKey, context.getData());
        String next = confidence >= threshold ? onHigh : onLow;
        return TransitionResult.continueTo(next);
    }
}
