package io.nexflow.engine.app.config;

import io.nexflow.engine.core.tenant.TenantContextHolder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;

/**
 * Enables the tenant Hibernate filter at the start of each transaction so that
 * queries are automatically scoped to the current tenant (from TenantContextHolder).
 */
@Aspect
@Component
public class HibernateTenantFilterConfigurer {

    private static final String FILTER_NAME = "tenantFilter";
    private static final String PARAM_TENANT_ID = "tenantId";

    private final EntityManagerFactory entityManagerFactory;

    public HibernateTenantFilterConfigurer(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Around("@within(org.springframework.transaction.annotation.Transactional)")
    public Object enableTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
        if (em != null) {
            Session session = em.unwrap(Session.class);
            String tenantId = TenantContextHolder.get();
            if (tenantId != null && !tenantId.isBlank()) {
                session.enableFilter(FILTER_NAME).setParameter(PARAM_TENANT_ID, tenantId);
            }
        }
        return joinPoint.proceed();
    }
}
