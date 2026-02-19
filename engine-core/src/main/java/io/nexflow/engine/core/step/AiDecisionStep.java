package io.nexflow.engine.core.step;

import io.nexflow.engine.core.definition.StepDefinition;
import io.nexflow.engine.core.runtime.ExecutionContext;
import io.nexflow.engine.core.runtime.TransitionResult;

import java.util.Map;

/**
 * Built-in step: AI decision. Calls evaluator for confidence and branches on threshold.
 * Config: promptKey, confidenceThreshold (optional), onHighConfidence, onLowConfidence (step ids or branch keys).
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
        Map<String, Object> config = step.getConfig();
        if (config == null) config = Map.of();
        String promptKey = (String) config.get("promptKey");
        if (promptKey == null || promptKey.isBlank()) {
            throw new IllegalArgumentException("aiDecision step requires promptKey in config");
        }
        Number th = (Number) config.get("confidenceThreshold");
        double threshold = th != null ? th.doubleValue() : DEFAULT_THRESHOLD;
        if (threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("confidenceThreshold must be in [0, 1]");
        }
        Object onHighObj = config.get("onHighConfidence");
        Object onLowObj = config.get("onLowConfidence");
        String onHigh = onHighObj != null ? String.valueOf(onHighObj) : null;
        String onLow = onLowObj != null ? String.valueOf(onLowObj) : null;
        if (onHigh == null || onHigh.isBlank() || onLow == null || onLow.isBlank()) {
            throw new IllegalArgumentException("aiDecision step requires onHighConfidence and onLowConfidence in config");
        }
        Map<String, Integer> branches = step.getBranches();
        if (branches != null && branches.containsKey(onHigh)) onHigh = String.valueOf(branches.get(onHigh));
        if (branches != null && branches.containsKey(onLow)) onLow = String.valueOf(branches.get(onLow));

        double confidence = evaluator.evaluateConfidence(promptKey, context.getData());
        String next = confidence >= threshold ? onHigh : onLow;
        return TransitionResult.continueTo(next);
    }
}
