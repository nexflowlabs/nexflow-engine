package io.nexflow.engine.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Normalized step definition for a workflow.
 * One row per step; config stored as JSONB.
 */
@Entity
@Table(
    name = "workflow_step_definition",
    uniqueConstraints = @UniqueConstraint(columnNames = { "workflow_definition_id", "step_id" })
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_definition_id", nullable = false)
    private Long workflowDefinitionId;

    /** DSL integer step id (unique per workflow). */
    @Column(name = "step_id", nullable = false)
    private Integer stepId;

    /** User-visible step name. */
    @Column(name = "step_name")
    private String stepName;

    /** Registry lookup type (e.g. transform, condition, httpCall). */
    @Column(name = "step_type", nullable = false)
    private String stepType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "jsonb")
    private String configJson;

    /** Branch key -> target step_id (JSONB, e.g. {"success": 2, "true": 3, "false": 4}). Null value = workflow complete. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "branches_json", columnDefinition = "jsonb")
    private String branchesJson;

    /** Factory for tests and callers that don't use Lombok builder. */
    public static WorkflowStepDefinitionEntity of(Long workflowDefinitionId, int stepId, String stepName, String stepType, String configJson) {
        WorkflowStepDefinitionEntity e = new WorkflowStepDefinitionEntity();
        e.setWorkflowDefinitionId(workflowDefinitionId);
        e.setStepId(stepId);
        e.setStepName(stepName);
        e.setStepType(stepType);
        e.setConfigJson(configJson);
        return e;
    }
}
