package io.nexflow.engine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "step_execution")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StepExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long executionId;
    /** Workflow step id (graph node id). */
    private Integer stepId;

    private String status;
    private int attempt;

    @Column(columnDefinition = "jsonb")
    private String inputJson;

    @Column(columnDefinition = "jsonb")
    private String outputJson;

    private Instant startedAt;
    private Instant endedAt;

    // Explicit setters for compatibility when Lombok processor is not used
    public void setExecutionId(Long executionId) { this.executionId = executionId; }
    public void setStepId(Integer stepId) { this.stepId = stepId; }
    public void setStatus(String status) { this.status = status; }
    public void setAttempt(int attempt) { this.attempt = attempt; }
    public void setInputJson(String inputJson) { this.inputJson = inputJson; }
    public void setOutputJson(String outputJson) { this.outputJson = outputJson; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
}

