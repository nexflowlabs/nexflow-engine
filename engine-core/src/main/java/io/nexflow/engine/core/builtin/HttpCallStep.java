package io.nexflow.engine.core.builtin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import com.nexflow.sdk.core.WorkflowStep;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Built-in step: HTTP call. Config: method, url, headers (optional), body (optional).
 * Optional: waitForCallback (boolean), waitToken (prefix for token), waitTokenHeader (header name, e.g. X-Callback-Token).
 * When waitForCallback is true, returns WAIT after the call so workflow pauses until webhook resume.
 * waitToken in config is used as prefix only; executionId and UUID are appended so each execution gets a unique token.
 */
public final class HttpCallStep implements WorkflowStep {

    private static final String TYPE_HTTP_CALL = "httpCall";
    private static final String CONFIG_METHOD = "method";
    private static final String CONFIG_URL = "url";
    private static final String CONFIG_HEADERS = "headers";
    private static final String CONFIG_BODY = "body";
    private static final String OUTPUT_RESPONSE = "response";
    private static final String OUTPUT_STATUS_CODE = "statusCode";
    private static final String DEFAULT_METHOD = "GET";
    private static final String DEFAULT_TOKEN_PREFIX = "http";
    private static final String ERROR_REQUIRES_URL = "httpCall requires url";
    private static final String METHOD_POST = "POST";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_PATCH = "PATCH";

    private static final HttpClient DEFAULT_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient client;

    public HttpCallStep() {
        this(DEFAULT_CLIENT);
    }

    public HttpCallStep(HttpClient client) {
        this.client = client != null ? client : DEFAULT_CLIENT;
    }

    @Override
    public String getType() {
        return TYPE_HTTP_CALL;
    }

    @Override
    public StepResult execute(StepExecutionContext context) {
        Map<String, Object> config = context.getStepConfig();
        String method = (String) config.getOrDefault(CONFIG_METHOD, DEFAULT_METHOD);
        String urlStr = (String) config.get(CONFIG_URL);
        if (urlStr == null || urlStr.isBlank()) {
            return StepResult.failure(ERROR_REQUIRES_URL);
        }
        boolean waitForCallback = Boolean.TRUE.equals(config.get(StepConstants.CONFIG_WAIT_FOR_CALLBACK))
                || StepConstants.BOOLEAN_TRUE.equalsIgnoreCase(String.valueOf(config.get(StepConstants.CONFIG_WAIT_FOR_CALLBACK)));
        String waitToken = waitForCallback ? resolveWaitToken(config, context) : null;

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .timeout(Duration.ofSeconds(30));
            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) config.get(CONFIG_HEADERS);
            if (headers != null) {
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    builder.header(e.getKey(), e.getValue());
                }
            }
            if (waitToken != null) {
                String headerName = (String) config.get(StepConstants.CONFIG_WAIT_TOKEN_HEADER);
                if (headerName != null && !headerName.isBlank()) {
                    builder.header(headerName, waitToken);
                }
            }
            Object body = config.get(CONFIG_BODY);
            if (body != null && (METHOD_POST.equalsIgnoreCase(method) || METHOD_PUT.equalsIgnoreCase(method) || METHOD_PATCH.equalsIgnoreCase(method))) {
                String bodyStr = body instanceof String ? (String) body : body.toString();
                builder.method(method, HttpRequest.BodyPublishers.ofString(bodyStr, StandardCharsets.UTF_8));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = response.statusCode();
            String responseBody = response.body();
            if (code >= 200 && code < 300) {
                Map<String, Object> outputs = new HashMap<>();
                outputs.put(OUTPUT_RESPONSE, responseBody);
                outputs.put(OUTPUT_STATUS_CODE, code);
                String outcome = outcomeFromResponseBody(responseBody);
                outputs.put(StepConstants.OUTCOME_KEY, outcome);
                if (waitForCallback && waitToken != null) {
                    return StepResult.waitForEvent(waitToken, outputs);
                }
                return StepResult.success(outputs);
            }
            return StepResult.failure("HTTP " + code + ": " + responseBody);
        } catch (Exception e) {
            return StepResult.failure(e.getMessage());
        }
    }

    /**
     * If response body is JSON and contains a non-blank "outcome" field, return its string value;
     * otherwise return "success".
     */
    private static String outcomeFromResponseBody(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "success";
        }
        String trimmed = responseBody.trim();
        if (!trimmed.startsWith("{")) {
            return "success";
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> json = JSON.readValue(responseBody, Map.class);
            Object o = json != null ? json.get(StepConstants.OUTCOME_KEY) : null;
            if (o != null) {
                String s = o.toString().trim();
                if (!s.isEmpty()) {
                    return s;
                }
            }
        } catch (Exception ignored) {
            // not valid JSON or outcome missing: use default
        }
        return "success";
    }

    /** Builds a unique token: config waitToken (if set) as prefix, then executionId and UUID. */
    private static String resolveWaitToken(Map<String, Object> config, StepExecutionContext context) {
        String prefix = DEFAULT_TOKEN_PREFIX;
        Object token = config.get(StepConstants.CONFIG_WAIT_TOKEN);
        if (token != null && !token.toString().isBlank()) {
            prefix = token.toString();
        }
        return prefix + "-" + context.getExecutionId() + "-" + UUID.randomUUID();
    }
}
