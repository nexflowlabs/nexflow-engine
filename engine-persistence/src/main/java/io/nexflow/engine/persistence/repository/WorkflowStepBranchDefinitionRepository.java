package io.nexflow.engine.persistence.repository;

import io.nexflow.engine.persistence.entity.WorkflowStepBranchDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowStepBranchDefinitionRepository
        extends JpaRepository<WorkflowStepBranchDefinitionEntity, Long> {

    List<WorkflowStepBranchDefinitionEntity> findByWorkflowStepDefinitionId(Long workflowStepDefinitionId);

    /** Load all branches for steps belonging to a workflow (via step definition ids). */
    List<WorkflowStepBranchDefinitionEntity> findByWorkflowStepDefinitionIdIn(List<Long> workflowStepDefinitionIds);
}
