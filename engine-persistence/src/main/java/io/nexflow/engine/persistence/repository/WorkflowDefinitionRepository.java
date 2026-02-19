package io.nexflow.engine.persistence.repository;

import io.nexflow.engine.persistence.entity.WorkflowDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkflowDefinitionRepository
        extends JpaRepository<WorkflowDefinitionEntity, Long> {

    WorkflowDefinitionEntity findByNameAndVersion(String name, int version);

    @Query("SELECT d FROM WorkflowDefinitionEntity d WHERE d.name = :name ORDER BY d.version DESC")
    List<WorkflowDefinitionEntity> findByNameOrderByVersionDesc(@Param("name") String name);

    @Query("SELECT d FROM WorkflowDefinitionEntity d WHERE d.name = :name AND d.active = true AND d.status = :status")
    List<WorkflowDefinitionEntity> findByNameAndActiveTrueAndStatus(@Param("name") String name, @Param("status") String status);

    default WorkflowDefinitionEntity findByLatestVersion(String workflowName) {
        List<WorkflowDefinitionEntity> list = findByNameOrderByVersionDesc(workflowName);
        return list.isEmpty() ? null : list.get(0);
    }

    /** Active published definition for the given name (single row when constraint is respected). */
    default WorkflowDefinitionEntity findActivePublishedByName(String name) {
        List<WorkflowDefinitionEntity> list = findByNameAndActiveTrueAndStatus(name, WorkflowDefinitionEntity.STATUS_PUBLISHED);
        return list.isEmpty() ? null : list.get(0);
    }
}
