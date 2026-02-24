package io.nexflow.engine.core.builtin;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import com.nexflow.sdk.core.WorkflowStep;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Built-in step: emit event. Config: eventType; optional waitForCallback, waitToken (prefix).
 * When waitForCallback is true, returns WAIT so workflow pauses until webhook resume.
 * waitToken in config is used as prefix only; executionId and UUID are appended so each execution gets a unique token.
 */
public final class EmitEventStep implements WorkflowStep {

    private static final String TYPE_EMIT_EVENT = "emitEvent";
    private static final String CONFIG_EVENT_TYPE = "eventType";
    private static final String OUTPUT_EVENT_TYPE = "eventType";
    private static final String DEFAULT_TOKEN_PREFIX = "emit";
    private static final String ERROR_REQUIRES_EVENT_TYPE = "emitEvent requires eventType";

    private final EmitEventPublisher publisher;

    public EmitEventStep() {
        this(null);
    }

    public EmitEventStep(EmitEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public String getType() {
        return TYPE_EMIT_EVENT;
    }

    @Override
    public StepResult execute(StepExecutionContext context) {
        Map<String, Object> config = context.getStepConfig();
        String eventType = (String) config.get(CONFIG_EVENT_TYPE);
        if (eventType == null || eventType.isBlank()) {
            return StepResult.failure(ERROR_REQUIRES_EVENT_TYPE);
        }
        boolean waitForCallback = Boolean.TRUE.equals(config.get(StepConstants.CONFIG_WAIT_FOR_CALLBACK))
                || StepConstants.BOOLEAN_TRUE.equalsIgnoreCase(String.valueOf(config.get(StepConstants.CONFIG_WAIT_FOR_CALLBACK)));
        String waitToken = waitForCallback ? resolveWaitToken(config, context) : null;

        if (publisher != null) {
            Map<String, Object> payload = new HashMap<>(context.getInputs());
            if (waitToken != null) {
                payload.put(StepConstants.PAYLOAD_WAIT_TOKEN, waitToken);
            }
            publisher.emit(eventType, payload);
        }

        if (waitForCallback && waitToken != null) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put(OUTPUT_EVENT_TYPE, eventType);
            return StepResult.waitForEvent(waitToken, metadata);
        }
        return StepResult.success(Map.of(StepConstants.OUTCOME_KEY, "success"));
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
