package io.nexflow.engine.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
    name = "workflow_definition",
    uniqueConstraints = @UniqueConstraint(columnNames = { "name", "version" })
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int version;
    private String description;

    /** First step to run (DSL integer step_id). Required for PUBLISHED. */
    @Column(name = "start_step_id")
    private Integer startStepId;

    /** DRAFT or PUBLISHED. */
    @Column(nullable = false)
    private String status;

    /** Whether this version is the active one for the name. */
    @Column(nullable = false)
    private boolean active;

    private Instant createdAt;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
}

