package io.nexflow.engine.persistence.mapper;

import io.nexflow.engine.core.domain.WorkflowExecution;
import io.nexflow.engine.persistence.entity.WorkflowExecutionEntity;
import org.springframework.stereotype.Component;

/**
 * Maps between workflow execution domain model and persistence entity.
 * Persistence layer must map to domain models; this is the single place for that mapping.
 */
@Component
public class ExecutionMapper {

    public WorkflowExecution toDomain(WorkflowExecutionEntity entity) {
        return entity == null ? null : entity.toDomain();
    }

    public WorkflowExecutionEntity toEntity(WorkflowExecution domain) {
        return domain == null ? null : WorkflowExecutionEntity.fromDomain(domain);
    }
}
