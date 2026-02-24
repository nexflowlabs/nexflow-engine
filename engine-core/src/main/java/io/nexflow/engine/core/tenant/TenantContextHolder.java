package io.nexflow.engine.core.tenant;

/**
 * Thread-local holder for the current tenant identifier.
 * Set by the app's TenantResolverFilter from request header or default in single-tenant mode.
 * Cleared after request; must be set again in async worker threads from the entity's tenantId.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static void set(String tenantId) {
        CONTEXT.set(tenantId);
    }

    public static String get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
