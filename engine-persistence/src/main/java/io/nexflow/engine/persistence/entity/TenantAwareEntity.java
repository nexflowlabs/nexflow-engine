package io.nexflow.engine.persistence.entity;

import io.nexflow.engine.core.tenant.TenantContextHolder;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Base for entities that are isolated by tenant.
 * Subclasses get automatic tenant_id assignment on persist and Hibernate filter
 * so queries only see rows for the current tenant.
 */
@MappedSuperclass
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = String.class)
)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantAwareEntity {

    @Column(name = "tenant_id", nullable = false, length = 100)
    protected String tenantId;

    @PrePersist
    public void assignTenant() {
        String current = TenantContextHolder.get();
        if (current == null || current.isBlank()) {
            current = "default";
        }
        this.tenantId = current;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
