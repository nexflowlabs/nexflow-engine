package io.nexflow.engine.core.step;

import java.util.Map;

/**
 * Provides confidence score (0.0–1.0) for AI decision steps.
 * Implemented by engine-app (e.g. HTTP client to AI service); engine-core only calls this.
 */
public interface AiDecisionEvaluator {

    /**
     * Evaluate confidence for the given prompt/context. Returns value in [0.0, 1.0].
     */
    double evaluateConfidence(String promptKey, Map<String, Object> context);
}
