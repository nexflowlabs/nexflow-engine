package io.nexflow.engine.core.script;

import java.util.Map;

/**
 * Structured result from script execution (in-process or isolated worker).
 * Only SUCCESS, FAILURE, or BRANCH; no arbitrary objects.
 */
public final class ScriptExecutionResult {

    public enum Status { SUCCESS, FAILURE, BRANCH }

    private final Status status;
    private final String branchName;
    private final Map<String, Object> outputs;
    private final String errorMessage;

    private ScriptExecutionResult(Status status, String branchName, Map<String, Object> outputs, String errorMessage) {
        this.status = status;
        this.branchName = branchName;
        this.outputs = outputs != null ? Map.copyOf(outputs) : Map.of();
        this.errorMessage = errorMessage;
    }

    public static ScriptExecutionResult success(Map<String, Object> outputs) {
        return new ScriptExecutionResult(Status.SUCCESS, null, outputs, null);
    }

    public static ScriptExecutionResult failure(String errorMessage) {
        return new ScriptExecutionResult(Status.FAILURE, null, null, errorMessage);
    }

    public static ScriptExecutionResult branch(String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("branchName cannot be null or blank");
        }
        return new ScriptExecutionResult(Status.BRANCH, branchName, null, null);
    }

    public Status getStatus() { return status; }
    public String getBranchName() { return branchName; }
    public Map<String, Object> getOutputs() { return outputs; }
    public String getErrorMessage() { return errorMessage; }
}
