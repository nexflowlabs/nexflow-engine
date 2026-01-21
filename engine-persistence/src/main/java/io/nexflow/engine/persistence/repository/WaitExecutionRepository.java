package io.nexflow.engine.persistence.repository;

import io.nexflow.engine.persistence.entity.WaitExecutionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WaitExecutionRepository
        extends JpaRepository<WaitExecutionEntity, UUID> {

    List<WaitExecutionEntity>
    findByStatusAndWaitTypeAndWaitUntilBefore(
            String status,
            String waitType,
            Instant now
    );

    List<WaitExecutionEntity>
    findByStatusAndWaitTypeAndEventName(
            String status,
            String waitType,
            String eventName
    );
}

