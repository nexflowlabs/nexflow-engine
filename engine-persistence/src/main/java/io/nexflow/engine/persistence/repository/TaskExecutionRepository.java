package io.nexflow.engine.persistence.repository;

import io.nexflow.engine.persistence.entity.TaskExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskExecutionRepository
        extends JpaRepository<TaskExecutionEntity, Long> {

    Optional<TaskExecutionEntity> findFirstByStatusOrderByCreatedAtAsc(String status);
}

