package io.nexflow.engine.persistence.repository;

import io.nexflow.engine.persistence.entity.WorkflowStepDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowStepDefinitionRepository
        extends JpaRepository<WorkflowStepDefinitionEntity, Long> {

    List<WorkflowStepDefinitionEntity> findByWorkflowDefinitionIdOrderByStepId(Long workflowDefinitionId);
}
