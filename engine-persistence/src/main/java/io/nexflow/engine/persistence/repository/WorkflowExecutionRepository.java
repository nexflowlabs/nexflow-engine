package io.nexflow.engine.persistence.repository;

import io.nexflow.engine.persistence.entity.WorkflowExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkflowExecutionRepository
        extends JpaRepository<WorkflowExecutionEntity, UUID> {

}

