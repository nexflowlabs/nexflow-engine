package io.nexflow.engine.persistence.repository;

import io.nexflow.engine.persistence.entity.TaskExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TaskExecutionRepository
        extends JpaRepository<TaskExecutionEntity, UUID> {

    Optional<TaskExecutionEntity> findFirstByStatusOrderByCreatedAtAsc(String status);
}

