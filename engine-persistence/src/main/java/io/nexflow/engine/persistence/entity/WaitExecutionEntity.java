package io.nexflow.engine.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wait_execution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitExecutionEntity {

    @Id
    private UUID id;

    private UUID executionId;
    private String stepName;
    private String waitType;     // TIME / EVENT

    private Instant waitUntil;   // TIME-based
    private String eventName;    // EVENT-based

    private String status;       // WAITING / COMPLETED

    private Instant createdAt;
    private Instant updatedAt;
}

