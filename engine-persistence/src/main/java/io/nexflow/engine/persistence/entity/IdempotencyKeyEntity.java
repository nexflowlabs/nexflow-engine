package io.nexflow.engine.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String scope;

    @Column(name = "key_value", nullable = false)
    private String key;

    private Long referenceId;

    private Instant createdAt;

    // Explicit getters/setters for compatibility when Lombok processor is not used
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public void setScope(String scope) { this.scope = scope; }
    public void setKey(String key) { this.key = key; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setId(Long id) { this.id = id; }

    public static IdempotencyKeyEntityBuilder builder() {
        return new IdempotencyKeyEntityBuilder();
    }

    public static final class IdempotencyKeyEntityBuilder {
        private Long id;
        private String scope;
        private String key;
        private Long referenceId;
        private Instant createdAt;

        IdempotencyKeyEntityBuilder() {}

        public IdempotencyKeyEntityBuilder scope(String scope) { this.scope = scope; return this; }
        public IdempotencyKeyEntityBuilder key(String key) { this.key = key; return this; }
        public IdempotencyKeyEntityBuilder referenceId(Long referenceId) { this.referenceId = referenceId; return this; }
        public IdempotencyKeyEntityBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public IdempotencyKeyEntity build() {
            IdempotencyKeyEntity e = new IdempotencyKeyEntity();
            e.setId(id);
            e.setScope(scope);
            e.setKey(key);
            e.setReferenceId(referenceId);
            e.setCreatedAt(createdAt);
            return e;
        }
    }
}
