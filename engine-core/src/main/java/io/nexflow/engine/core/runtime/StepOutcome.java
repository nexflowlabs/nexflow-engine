package io.nexflow.engine.core.runtime;

import java.util.HashMap;
import java.util.Map;

/**
 * Result of running one step: next step id, or complete/fail/wait.
 * Produced by {@link RegistryDrivenRuntime#runStep}; consumed by the workflow runtime service.
 */
public final class StepOutcome {

    public enum Type { CONTINUE, COMPLETE, FAIL, WAIT }

    private final Type type;
    private final Integer nextStepId;
    private final Map<String, Object> contextUpdates;
    private final String waitToken;
    private final String errorMessage;

    private StepOutcome(Type type, Integer nextStepId, Map<String, Object> contextUpdates, String waitToken, String errorMessage) {
        this.type = type;
        this.nextStepId = nextStepId;
        this.contextUpdates = contextUpdates != null ? new HashMap<>(contextUpdates) : Map.of();
        this.waitToken = waitToken;
        this.errorMessage = errorMessage;
    }

    public static StepOutcome continueTo(Integer nextStepId, Map<String, Object> contextUpdates) {
        return new StepOutcome(Type.CONTINUE, nextStepId, contextUpdates, null, null);
    }

    public static StepOutcome complete(Map<String, Object> contextUpdates) {
        return new StepOutcome(Type.COMPLETE, null, contextUpdates, null, null);
    }

    public static StepOutcome fail() {
        return new StepOutcome(Type.FAIL, null, null, null, null);
    }

    /** Fail with an error message (e.g. unknown branch). */
    public static StepOutcome fail(String errorMessage) {
        return new StepOutcome(Type.FAIL, null, null, null, errorMessage != null ? errorMessage : null);
    }

    /** Pause execution until webhook/event resumes; contextUpdates are merged before pausing. */
    public static StepOutcome waitForEvent(String waitToken, Map<String, Object> contextUpdates) {
        if (waitToken == null || waitToken.isBlank()) {
            throw new IllegalArgumentException("waitToken cannot be null or blank");
        }
        return new StepOutcome(Type.WAIT, null, contextUpdates != null ? contextUpdates : Map.of(), waitToken, null);
    }

    public Type getType() { return type; }
    public Integer getNextStepId() { return nextStepId; }
    public Map<String, Object> getContextUpdates() { return contextUpdates; }
    public String getWaitToken() { return waitToken; }
    /** Error message when type is FAIL (e.g. unknown branch). */
    public String getErrorMessage() { return errorMessage; }
}
