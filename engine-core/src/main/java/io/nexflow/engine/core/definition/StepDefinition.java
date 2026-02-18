package io.nexflow.engine.core.definition;

public class StepDefinition {

    private StepType type;

    // TASK
    private String task;
    private RetryPolicy retry;
    private String onSuccess;
    private String onFailure;

    // DECISION
    private String expression;
    private String onTrue;
    private String onFalse;

    // WAIT
    private String duration;
    private String event;
    private String next;

    // END
    private String status;

    // AI_DECISION
    private String promptKey;
    private Double confidenceThreshold;
    private String onHighConfidence;
    private String onLowConfidence;

    public StepType getType() {
        return type;
    }

    public void setType(StepType type) {
        this.type = type;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public RetryPolicy getRetry() {
        return retry;
    }

    public void setRetry(RetryPolicy retry) {
        this.retry = retry;
    }

    public String getOnSuccess() {
        return onSuccess;
    }

    public void setOnSuccess(String onSuccess) {
        this.onSuccess = onSuccess;
    }

    public String getOnFailure() {
        return onFailure;
    }

    public void setOnFailure(String onFailure) {
        this.onFailure = onFailure;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getOnTrue() {
        return onTrue;
    }

    public void setOnTrue(String onTrue) {
        this.onTrue = onTrue;
    }

    public String getOnFalse() {
        return onFalse;
    }

    public void setOnFalse(String onFalse) {
        this.onFalse = onFalse;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPromptKey() { return promptKey; }
    public void setPromptKey(String promptKey) { this.promptKey = promptKey; }
    public Double getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(Double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
    public String getOnHighConfidence() { return onHighConfidence; }
    public void setOnHighConfidence(String onHighConfidence) { this.onHighConfidence = onHighConfidence; }
    public String getOnLowConfidence() { return onLowConfidence; }
    public void setOnLowConfidence(String onLowConfidence) { this.onLowConfidence = onLowConfidence; }
}

