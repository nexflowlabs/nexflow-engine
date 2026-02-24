package io.nexflow.engine.tenant;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tenant configuration: single-tenant (self-hosted) or multi-tenant (SaaS).
 */
@ConfigurationProperties(prefix = "nexflow.tenant")
public class TenantProperties {

    /** single = use default-id when X-Tenant-Id absent; multi = require X-Tenant-Id (or use default). */
    private String mode = "single";
    /** Default tenant when mode is single or when header is missing in multi. */
    private String defaultId = "default";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDefaultId() {
        return defaultId;
    }

    public void setDefaultId(String defaultId) {
        this.defaultId = defaultId;
    }
}
