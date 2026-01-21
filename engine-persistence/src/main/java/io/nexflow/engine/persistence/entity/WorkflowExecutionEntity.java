package io.nexflow.engine.persistence.entity;

import jakarta.persistence.Column;
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
@Table(name = "workflow_execution")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecutionEntity {

    @Id
    private UUID id;

    private String workflowName;
    private int workflowVersion;

    private String status;
    private String currentStep;

    @Column(columnDefinition = "jsonb")
    private String contextJson;

    private Instant startedAt;
    private Instant updatedAt;
}

