package io.nexflow.engine.app.publish;

import io.nexflow.engine.app.loader.NormalizedWorkflowLoader;
import io.nexflow.engine.app.validation.WorkflowDefinitionValidationException;
import io.nexflow.engine.core.definition.StepDefinition;
import io.nexflow.engine.core.definition.WorkflowDefinition;
import io.nexflow.engine.core.registry.StepRegistry;
import io.nexflow.engine.persistence.entity.WorkflowDefinitionEntity;
import io.nexflow.engine.persistence.entity.WorkflowStepBranchDefinitionEntity;
import io.nexflow.engine.persistence.entity.WorkflowStepDefinitionEntity;
import io.nexflow.engine.persistence.repository.WorkflowDefinitionRepository;
import io.nexflow.engine.persistence.repository.WorkflowStepBranchDefinitionRepository;
import io.nexflow.engine.persistence.repository.WorkflowStepDefinitionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validates and publishes workflow definitions.
 * Before publish: unique step_id, startStepId exists, branch targets exist, step_type in registry, no dangling branches.
 */
@Service
@RequiredArgsConstructor
public class WorkflowPublishService {

    private final WorkflowDefinitionRepository definitionRepo;
    private final WorkflowStepDefinitionRepository stepDefRepo;
    private final WorkflowStepBranchDefinitionRepository branchDefRepo;
    private final NormalizedWorkflowLoader workflowLoader;
    private final StepRegistry stepRegistry;

    /**
     * Validates the workflow definition for publish. Throws if invalid.
     */
    public void validateForPublish(Long workflowDefinitionId) {
        List<String> errors = new ArrayList<>();

        WorkflowDefinitionEntity def = definitionRepo.findById(workflowDefinitionId).orElse(null);
        if (def == null) {
            errors.add("Workflow definition not found: " + workflowDefinitionId);
            throw new WorkflowDefinitionValidationException(errors);
        }

        List<WorkflowStepDefinitionEntity> steps = stepDefRepo.findByWorkflowDefinitionIdOrderByStepId(workflowDefinitionId);
        Set<Integer> stepIds = steps.stream().map(WorkflowStepDefinitionEntity::getStepId).collect(Collectors.toSet());

        if (steps.isEmpty()) {
            errors.add("Workflow has no steps");
        }

        if (stepIds.size() != steps.size()) {
            errors.add("Duplicate step_id in workflow");
        }

        Integer startStepId = def.getStartStepId();
        if (startStepId == null) {
            errors.add("startStepId is required for publish");
        } else if (!stepIds.contains(startStepId)) {
            errors.add("startStepId " + startStepId + " does not match any step");
        }

        List<Long> stepDefIds = steps.stream().map(WorkflowStepDefinitionEntity::getId).toList();
        List<WorkflowStepBranchDefinitionEntity> branches = branchDefRepo.findByWorkflowStepDefinitionIdIn(stepDefIds);
        for (WorkflowStepBranchDefinitionEntity b : branches) {
            if (!stepIds.contains(b.getTargetStepId())) {
                errors.add("Branch target_step_id " + b.getTargetStepId() + " does not exist (dangling branch)");
            }
        }

        for (WorkflowStepDefinitionEntity se : steps) {
            String stepType = se.getStepType();
            if (stepType == null || stepType.isBlank()) {
                errors.add("Step " + se.getStepId() + " has no step_type");
            } else if (stepRegistry.get(stepType) == null) {
                errors.add("Step type not in registry: " + stepType + " (step_id=" + se.getStepId() + ")");
            }
        }

        if (!errors.isEmpty()) {
            throw new WorkflowDefinitionValidationException(errors);
        }
    }

    /**
     * Validates then sets status to PUBLISHED and this definition to active; deactivates other versions of the same name.
     */
    @Transactional
    public void publish(Long workflowDefinitionId) {
        validateForPublish(workflowDefinitionId);
        WorkflowDefinitionEntity def = definitionRepo.findById(workflowDefinitionId).orElseThrow();
        List<WorkflowDefinitionEntity> byName = definitionRepo.findByNameOrderByVersionDesc(def.getName());
        for (WorkflowDefinitionEntity other : byName) {
            if (!other.getId().equals(workflowDefinitionId)) {
                other.setActive(false);
                definitionRepo.save(other);
            }
        }
        def.setStatus(WorkflowDefinitionEntity.STATUS_PUBLISHED);
        def.setActive(true);
        definitionRepo.save(def);
    }
}
