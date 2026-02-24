package io.nexflow.engine.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "wait_execution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitExecutionEntity extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long executionId;
    private Integer stepId;
    private String waitType;     // TIME / EVENT

    private Instant waitUntil;   // TIME-based
    private String eventName;    // EVENT-based
    /** Unique token for webhook resume (EVENT waits). Lookup via findByWaitToken. */
    private String waitToken;
    /** Default next step id to run when this wait is resumed (from step def). */
    private Integer nextStepIdWhenResumed;

    private String status;       // WAITING / COMPLETED

    private Instant createdAt;
    private Instant updatedAt;

}

