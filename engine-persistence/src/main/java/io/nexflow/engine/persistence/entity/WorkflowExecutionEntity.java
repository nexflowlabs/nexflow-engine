package io.nexflow.engine.persistence.entity;

import io.nexflow.engine.core.domain.WorkflowExecution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_execution")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecutionEntity {

    @Id
    private UUID id;

    private String workflowName;
    private int workflowVersion;

    private String status;
    private String currentStep;

    @Column(columnDefinition = "jsonb")
    private String contextJson;

    private Instant startedAt;
    private Instant updatedAt;

    /** Map to domain model (engine-core). */
    public WorkflowExecution toDomain() {
        WorkflowExecution d = new WorkflowExecution();
        d.setId(this.id);
        d.setWorkflowName(this.workflowName);
        d.setWorkflowVersion(this.workflowVersion);
        d.setStatus(this.status);
        d.setCurrentStep(this.currentStep);
        d.setContextJson(this.contextJson);
        d.setStartedAt(this.startedAt);
        d.setUpdatedAt(this.updatedAt);
        return d;
    }

    /** Create entity from domain model. */
    public static WorkflowExecutionEntity fromDomain(WorkflowExecution d) {
        return builder()
                .id(d.getId())
                .workflowName(d.getWorkflowName())
                .workflowVersion(d.getWorkflowVersion())
                .status(d.getStatus())
                .currentStep(d.getCurrentStep())
                .contextJson(d.getContextJson())
                .startedAt(d.getStartedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}

