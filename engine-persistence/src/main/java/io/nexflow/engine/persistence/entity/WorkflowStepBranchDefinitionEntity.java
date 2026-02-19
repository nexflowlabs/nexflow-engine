package io.nexflow.engine.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Branch from one step to another (e.g. condition TRUE -> step 2, FALSE -> step 3).
 */
@Entity
@Table(
    name = "workflow_step_branch_definition",
    uniqueConstraints = @UniqueConstraint(columnNames = { "workflow_step_definition_id", "branch_key" })
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepBranchDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Step this branch belongs to (source). */
    @Column(name = "workflow_step_definition_id", nullable = false)
    private Long workflowStepDefinitionId;

    /** Branch key (e.g. "TRUE", "FALSE", "HIGH", "LOW"). */
    @Column(name = "branch_key", nullable = false)
    private String branchKey;

    /** Target step (DSL integer id). Null means workflow complete for this branch. */
    @Column(name = "target_step_id")
    private Integer targetStepId;

    /** Factory for tests and callers that don't use Lombok builder. */
    public static WorkflowStepBranchDefinitionEntity of(Long workflowStepDefinitionId, String branchKey, int targetStepId) {
        return of(workflowStepDefinitionId, branchKey, Integer.valueOf(targetStepId));
    }

    /** Factory allowing null target (workflow complete for this branch). */
    public static WorkflowStepBranchDefinitionEntity of(Long workflowStepDefinitionId, String branchKey, Integer targetStepId) {
        WorkflowStepBranchDefinitionEntity e = new WorkflowStepBranchDefinitionEntity();
        e.setWorkflowStepDefinitionId(workflowStepDefinitionId);
        e.setBranchKey(branchKey);
        e.setTargetStepId(targetStepId);
        return e;
    }
}
