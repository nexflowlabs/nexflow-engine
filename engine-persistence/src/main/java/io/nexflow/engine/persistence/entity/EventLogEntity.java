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
@Table(name = "event_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventLogEntity {

    @Id
    private UUID id;

    private UUID executionId;
    private String eventType;

    @Column(columnDefinition = "jsonb")
    private String payloadJson;

    private Instant createdAt;
}