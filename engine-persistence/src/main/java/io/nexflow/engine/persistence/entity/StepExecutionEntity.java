package io.nexflow.engine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "step_execution")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StepExecutionEntity {

    @Id
    private UUID id;

    private UUID executionId;
    private String stepName;

    private String status;
    private int attempt;

    @Column(columnDefinition = "jsonb")
    private String inputJson;

    @Column(columnDefinition = "jsonb")
    private String outputJson;

    private Instant startedAt;
    private Instant endedAt;
}

