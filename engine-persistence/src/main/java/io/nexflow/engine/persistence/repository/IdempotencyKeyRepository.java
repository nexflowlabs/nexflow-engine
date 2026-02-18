package io.nexflow.engine.persistence.repository;

import io.nexflow.engine.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository
        extends JpaRepository<IdempotencyKeyEntity, UUID> {

    Optional<IdempotencyKeyEntity>
    findByScopeAndKey(String scope, String key);
}
