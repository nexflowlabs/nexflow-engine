package io.nexflow.engine.persistence.repository;

import io.nexflow.engine.persistence.entity.StepExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StepExecutionRepository
        extends JpaRepository<StepExecutionEntity, UUID> {

}

