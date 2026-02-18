package io.nexflow.engine.core.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model for a workflow execution.
 * Persistence layer maps to/from WorkflowExecutionEntity; app logic should use this type.
 */
public class WorkflowExecution {

    private UUID id;
    private String workflowName;
    private int workflowVersion;
    private String status;
    private String currentStep;
    private String contextJson;
    private Instant startedAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
    public int getWorkflowVersion() { return workflowVersion; }
    public void setWorkflowVersion(int workflowVersion) { this.workflowVersion = workflowVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
