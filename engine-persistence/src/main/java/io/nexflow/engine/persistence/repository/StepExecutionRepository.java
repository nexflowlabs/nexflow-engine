package io.nexflow.engine.persistence.repository;

import io.nexflow.engine.persistence.entity.StepExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StepExecutionRepository
        extends JpaRepository<StepExecutionEntity, Long> {

    /** List all step executions for a workflow execution, in start order. */
    List<StepExecutionEntity> findByExecutionIdOrderByStartedAtAsc(Long executionId);

    /** Count how many times this step has been run for this execution (for retry attempt number). */
    long countByExecutionIdAndStepId(Long executionId, Integer stepId);
}

