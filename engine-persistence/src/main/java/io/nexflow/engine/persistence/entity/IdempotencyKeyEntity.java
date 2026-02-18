package io.nexflow.engine.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "idempotency_key",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"scope", "key_value"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKeyEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String scope;

    @Column(name = "key_value", nullable = false)
    private String key;

    private UUID referenceId;

    private Instant createdAt;
}
