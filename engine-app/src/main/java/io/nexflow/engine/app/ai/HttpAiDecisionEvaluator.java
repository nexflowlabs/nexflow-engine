package io.nexflow.engine.app.ai;

import io.nexflow.engine.core.step.AiDecisionEvaluator;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Calls an external HTTP endpoint for AI confidence. Expects JSON body with promptKey and context,
 * returns JSON with "confidence" (0.0–1.0).
 */
public class HttpAiDecisionEvaluator implements AiDecisionEvaluator {

    private final RestClient restClient;

    public HttpAiDecisionEvaluator(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public double evaluateConfidence(String promptKey, Map<String, Object> context) {
        Map<String, Object> request = Map.of(
                "promptKey", promptKey,
                "context", context != null ? context : Map.of()
        );
        Map<?, ?> response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);
        if (response == null || !response.containsKey("confidence")) {
            return 0.0;
        }
        Object c = response.get("confidence");
        if (c instanceof Number) {
            return ((Number) c).doubleValue();
        }
        return 0.0;
    }
}
