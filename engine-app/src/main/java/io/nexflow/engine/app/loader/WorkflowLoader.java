package io.nexflow.engine.app.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexflow.engine.core.definition.StepDefinition;
import io.nexflow.engine.core.definition.WorkflowDefinition;
import io.nexflow.engine.persistence.entity.WorkflowDefinitionEntity;
import io.nexflow.engine.persistence.entity.WorkflowStepDefinitionEntity;
import io.nexflow.engine.persistence.repository.WorkflowDefinitionRepository;
import io.nexflow.engine.persistence.repository.WorkflowStepDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Loads a workflow by workflow_definition_id and builds an in-memory graph from
 * workflow_definition and workflow_step_definition (branches in branches_json).
 * No DB queries during step transitions.
 */
@Component
@RequiredArgsConstructor
public class WorkflowLoader {

    private final WorkflowDefinitionRepository definitionRepo;
    private final WorkflowStepDefinitionRepository stepDefRepo;
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

        List<StepDefinition> stepDefs = new ArrayList<>();
        for (WorkflowStepDefinitionEntity se : steps) {
            Map<String, Object> config = parseConfig(se.getConfigJson());
            Map<String, Integer> branchMap = parseBranches(se.getBranchesJson());
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

    /** Parse branches_json to branchKey -> targetStepId (null = workflow complete). */
    private Map<String, Integer> parseBranches(String branchesJson) {
        if (branchesJson == null || branchesJson.isBlank()) return Map.of();
        try {
            Map<String, Object> raw = objectMapper.readValue(branchesJson, new TypeReference<Map<String, Object>>() {});
            if (raw == null) return Map.of();
            Map<String, Integer> out = new HashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                if (e.getKey() == null) continue;
                if (e.getValue() == null) {
                    out.put(e.getKey(), null);
                } else if (e.getValue() instanceof Number n) {
                    out.put(e.getKey(), n.intValue());
                }
            }
            return out;
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** In-memory workflow graph + start step id. Internal to loader; not exposed to callers. */
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
