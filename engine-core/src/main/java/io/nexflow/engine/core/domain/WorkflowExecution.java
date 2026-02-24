package io.nexflow.engine.core.domain;

import java.time.Instant;

/**
 * Domain model for a workflow execution.
 * Persistence layer maps to/from WorkflowExecutionEntity; app logic should use this type.
 */
public class WorkflowExecution {

    private Long id;
    private Long workflowDefinitionId;
    private String status;
    private Integer currentStepId;
    private String contextJson;
    private Instant startedAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkflowDefinitionId() { return workflowDefinitionId; }
    public void setWorkflowDefinitionId(Long workflowDefinitionId) { this.workflowDefinitionId = workflowDefinitionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getCurrentStepId() { return currentStepId; }
    public void setCurrentStepId(Integer currentStepId) { this.currentStepId = currentStepId; }
    public String getContextJson() { return contextJson; }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
