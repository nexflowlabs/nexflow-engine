package io.nexflow.engine.core.definition;

import java.util.Map;

/**
 * v1 DSL step: resolved by "name" in StepRegistry; "id" is the step identifier.
 * Transitions are via branches only: map of branchName -> target step id (null = workflow complete).
 */
public class StepDefinition {
    private final int id;
    private final String stepName;  // user-defined
    private final String type;      // registry lookup
    private final Map<String, Object> config;
    private final Map<String, Integer> branches;  // branchName -> target step id; null value = complete

    public StepDefinition(int id, String stepName, String type, Map<String, Object> config, Map<String, Integer> branches) {
        this.id = id;
        this.stepName = stepName != null ? stepName : "";
        this.type = type != null ? type : "";
        this.config = config != null ? config : Map.of();
        this.branches = branches != null ? branches : Map.of();
    }

    public int getId() { return id; }
    public String getStepName() { return stepName; }
    public String getType() { return type; }
    public Map<String, Object> getConfig() { return config; }
    public Map<String, Integer> getBranches() { return branches; }
}
