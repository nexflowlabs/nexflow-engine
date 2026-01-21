package io.nexflow.engine.persistence.repository;

import io.nexflow.engine.persistence.entity.WorkflowDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkflowDefinitionRepository
        extends JpaRepository<WorkflowDefinitionEntity, UUID> {

    WorkflowDefinitionEntity findByNameAndVersion(String workflowName, int workflowVersion);
}
