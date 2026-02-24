package io.nexflow.engine.tenant;

import io.nexflow.engine.core.tenant.TenantContextHolder;
import io.nexflow.engine.persistence.entity.TenantAwareEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Security enforcement: verify that an entity's tenant matches the current context
 * before returning workflow/execution to the client. Use in controllers when returning
 * workflow or execution by id.
 */
public final class TenantVerification {

    private TenantVerification() {}

    /**
     * Throws 403 if the entity's tenantId does not match TenantContextHolder.get().
     * Call this before returning an entity to the client (e.g. in GET /executions/{id}).
     */
    public static void ensureTenantMatch(TenantAwareEntity entity) {
        if (entity == null) return;
        String current = TenantContextHolder.get();
        String entityTenant = entity.getTenantId();
        if (entityTenant != null && current != null && !entityTenant.equals(current)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tenant mismatch");
        }
    }
}
