package io.nexflow.engine.app.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexflow.engine.core.script.ScriptExecutionClient;
import io.nexflow.engine.core.script.ScriptExecutionResult;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP client for script execution in an isolated worker.
 * POSTs to nexflow.script.worker-endpoint with language, code, inputs, stepConfig, timeoutMs.
 * Expects JSON response: { "status": "SUCCESS"|"FAILURE"|"BRANCH", "outputs"?: {}, "branch"?: "", "errorMessage"?: "" }.
 */
public class HttpScriptExecutionClient implements ScriptExecutionClient {

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HttpScriptExecutionClient(String workerEndpoint, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.baseUrl = workerEndpoint == null ? "" : workerEndpoint.trim();
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public ScriptExecutionResult execute(String language, String code, Map<String, Object> inputs,
                                         Map<String, Object> stepConfig, long timeoutMs) {
        var body = Map.<String, Object>of(
                "language", language != null ? language : "javascript",
                "code", code != null ? code : "",
                "inputs", inputs != null ? inputs : Map.of(),
                "stepConfig", stepConfig != null ? stepConfig : Map.of(),
                "timeoutMs", timeoutMs
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, request, String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return parseResponse(response.getBody());
        }
        return ScriptExecutionResult.failure("worker returned " + response.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    private ScriptExecutionResult parseResponse(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            String status = (String) map.get("status");
            if (status == null) return ScriptExecutionResult.failure("missing status");
            switch (status.toUpperCase()) {
                case "SUCCESS":
                    return ScriptExecutionResult.success((Map<String, Object>) map.get("outputs"));
                case "FAILURE":
                    return ScriptExecutionResult.failure((String) map.get("errorMessage"));
                case "BRANCH":
                    String branch = (String) map.get("branch");
                    if (branch == null || branch.isBlank()) return ScriptExecutionResult.failure("missing branch name");
                    return ScriptExecutionResult.branch(branch);
                default:
                    return ScriptExecutionResult.failure("invalid status: " + status);
            }
        } catch (Exception e) {
            return ScriptExecutionResult.failure("failed to parse worker response: " + e.getMessage());
        }
    }
}
