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
@Table(name = "workflow_definition")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinitionEntity {

    @Id
    private UUID id;

    private String name;
    private int version;
    private String description;

    @Column(columnDefinition = "jsonb")
    private String definitionJson;

    private Instant createdAt;
}

