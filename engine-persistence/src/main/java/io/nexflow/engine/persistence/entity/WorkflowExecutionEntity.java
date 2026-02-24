package io.nexflow.engine.persistence.entity;

import io.nexflow.engine.core.domain.WorkflowExecution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "workflow_execution")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecutionEntity extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_definition_id", nullable = false)
    private Long workflowDefinitionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", insertable = false, updatable = false)
    private WorkflowDefinitionEntity workflowDefinition;

    private String status;

    /** Current step (DSL integer step_id). Null when completed/failed. */
    @Column(name = "current_step_id")
    private Integer currentStepId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_json", columnDefinition = "jsonb")
    private String contextJson;

    private Instant startedAt;
    private Instant updatedAt;

    /** Map to domain model (engine-core). */
    public WorkflowExecution toDomain() {
        WorkflowExecution d = new WorkflowExecution();
        d.setId(this.id);
        d.setWorkflowDefinitionId(this.workflowDefinitionId);
        d.setStatus(this.status);
        d.setCurrentStepId(this.currentStepId);
        d.setContextJson(this.contextJson);
        d.setStartedAt(this.startedAt);
        d.setUpdatedAt(this.updatedAt);
        return d;
    }

    /** Create entity from domain model. */
    public static WorkflowExecutionEntity fromDomain(WorkflowExecution d) {
        return new WorkflowExecutionEntity(
                d.getId(),
                d.getWorkflowDefinitionId(),
                null,
                d.getStatus(),
                d.getCurrentStepId(),
                d.getContextJson(),
                d.getStartedAt(),
                d.getUpdatedAt()
        );
    }
}

