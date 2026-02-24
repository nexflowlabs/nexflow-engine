package io.nexflow.engine.tenant;

import io.nexflow.engine.core.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resolves tenant from request and sets {@link TenantContextHolder}.
 * In single-tenant mode uses configured default when X-Tenant-Id is absent;
 * in multi-tenant mode uses header or falls back to default.
 * Clears the holder after the request.
 */
@Order(1)
public class TenantResolverFilter extends OncePerRequestFilter {

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";

    private final TenantProperties tenantProperties;

    public TenantResolverFilter(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String tenantId = request.getHeader(HEADER_TENANT_ID);
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = tenantProperties.getDefaultId();
                if (tenantId == null || tenantId.isBlank()) {
                    tenantId = "default";
                }
            } else {
                tenantId = tenantId.trim();
            }
            TenantContextHolder.set(tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
