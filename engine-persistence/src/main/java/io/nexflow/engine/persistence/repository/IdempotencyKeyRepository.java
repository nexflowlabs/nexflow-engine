package io.nexflow.engine.persistence.repository;

import io.nexflow.engine.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository
        extends JpaRepository<IdempotencyKeyEntity, Long> {

    Optional<IdempotencyKeyEntity>
    findByScopeAndKey(String scope, String key);
}
