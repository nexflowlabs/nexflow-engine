package io.nexflow.engine.app.config;

import io.nexflow.engine.tenant.TenantProperties;
import io.nexflow.engine.tenant.TenantResolverFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TenantProperties.class)
public class TenantConfig {

    @Bean
    public FilterRegistrationBean<TenantResolverFilter> tenantResolverFilter(TenantProperties tenantProperties) {
        FilterRegistrationBean<TenantResolverFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantResolverFilter(tenantProperties));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
