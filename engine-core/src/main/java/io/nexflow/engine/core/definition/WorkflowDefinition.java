package io.nexflow.engine.core.definition;

import lombok.Data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * workflow DSL: registry-driven steps, no type enum.
 * Steps are stored in order; stepsById provides O(1) lookup by step id.
 */
@Data
public class WorkflowDefinition {
    private String name;
    private String version;
    private String description;
    private Trigger trigger;
    private List<StepDefinition> steps;
    /** O(1) lookup by step id; rebuilt when steps are set. */
    private Map<Integer, StepDefinition> stepsById = Map.of();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<StepDefinition> getSteps() { return steps; }
    public void setSteps(List<StepDefinition> steps) {
        this.steps = steps;
        if (steps == null || steps.isEmpty()) {
            this.stepsById = Map.of();
        } else {
            Map<Integer, StepDefinition> byId = new HashMap<>();
            for (StepDefinition s : steps) {
                byId.put(s.getId(), s);
            }
            this.stepsById = Collections.unmodifiableMap(byId);
        }
    }

    /** Returns the step with the given id, or null if not found. O(1). */
    public StepDefinition getStepById(Integer id) {
        if (id == null) return null;
        return stepsById.get(id);
    }
}
