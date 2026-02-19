package io.nexflow.engine.app.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexflow.engine.core.definition.StepDefinition;
import io.nexflow.engine.core.definition.WorkflowDefinition;
import io.nexflow.engine.persistence.entity.WorkflowDefinitionEntity;
import io.nexflow.engine.persistence.entity.WorkflowStepBranchDefinitionEntity;
import io.nexflow.engine.persistence.entity.WorkflowStepDefinitionEntity;
import io.nexflow.engine.persistence.repository.WorkflowDefinitionRepository;
import io.nexflow.engine.persistence.repository.WorkflowStepBranchDefinitionRepository;
import io.nexflow.engine.persistence.repository.WorkflowStepDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads a workflow by workflow_definition_id and builds an in-memory graph from
 * workflow_definition, workflow_step_definition, and workflow_step_branch_definition.
 * No DB queries during step transitions.
 */
@Component
@RequiredArgsConstructor
public class NormalizedWorkflowLoader {

    private final WorkflowDefinitionRepository definitionRepo;
    private final WorkflowStepDefinitionRepository stepDefRepo;
    private final WorkflowStepBranchDefinitionRepository branchDefRepo;
    private final ObjectMapper objectMapper;

    /**
     * Load workflow definition and all steps/branches, build in-memory graph.
     * Returns null if definition not found or not published (when requirePublished is true).
     */
    public LoadedWorkflow load(Long workflowDefinitionId, boolean requirePublished) {
        WorkflowDefinitionEntity def = definitionRepo.findById(workflowDefinitionId).orElse(null);
        if (def == null) return null;
        if (requirePublished && !WorkflowDefinitionEntity.STATUS_PUBLISHED.equals(def.getStatus())) {
            return null;
        }
        List<WorkflowStepDefinitionEntity> steps = stepDefRepo.findByWorkflowDefinitionIdOrderByStepId(workflowDefinitionId);
        if (steps.isEmpty()) return null;
        List<Long> stepDefIds = steps.stream().map(WorkflowStepDefinitionEntity::getId).toList();
        List<WorkflowStepBranchDefinitionEntity> branches = branchDefRepo.findByWorkflowStepDefinitionIdIn(stepDefIds);
        Map<Long, List<WorkflowStepBranchDefinitionEntity>> branchesByStepDefId = branches.stream()
                .collect(Collectors.groupingBy(WorkflowStepBranchDefinitionEntity::getWorkflowStepDefinitionId));

        List<StepDefinition> stepDefs = new ArrayList<>();
        for (WorkflowStepDefinitionEntity se : steps) {
            Map<String, Object> config = parseConfig(se.getConfigJson());
            List<WorkflowStepBranchDefinitionEntity> stepBranches = branchesByStepDefId.getOrDefault(se.getId(), List.of());
            Map<String, Integer> branchMap = new HashMap<>();
            for (WorkflowStepBranchDefinitionEntity b : stepBranches) {
                branchMap.put(b.getBranchKey(), b.getTargetStepId());
            }
            StepDefinition sd = new StepDefinition(
                    se.getStepId(),
                    se.getStepName() != null ? se.getStepName() : "",
                    se.getStepType(),
                    config != null ? config : Map.of(),
                    branchMap
            );
            stepDefs.add(sd);
        }

        WorkflowDefinition wf = new WorkflowDefinition();
        wf.setName(def.getName());
        wf.setVersion(String.valueOf(def.getVersion()));
        wf.setDescription(def.getDescription());
        wf.setSteps(stepDefs);
        return new LoadedWorkflow(def, wf, def.getStartStepId());
    }

    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** In-memory workflow graph + start step id. */
    public static final class LoadedWorkflow {
        private final WorkflowDefinitionEntity entity;
        private final WorkflowDefinition definition;
        private final Integer startStepId;

        public LoadedWorkflow(WorkflowDefinitionEntity entity, WorkflowDefinition definition, Integer startStepId) {
            this.entity = entity;
            this.definition = definition;
            this.startStepId = startStepId;
        }

        public WorkflowDefinitionEntity getEntity() { return entity; }
        public WorkflowDefinition getDefinition() { return definition; }
        public Integer getStartStepId() { return startStepId; }
    }
}
