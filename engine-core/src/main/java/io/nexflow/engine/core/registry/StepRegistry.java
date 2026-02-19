package io.nexflow.engine.core.registry;

import com.nexflow.sdk.core.WorkflowStep;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps step type (DSL "name"/type) to WorkflowStep implementation.
 * No type-based dispatch; all steps are resolved by name.
 */
public class StepRegistry {

    private final Map<String, WorkflowStep> stepByType;

    public StepRegistry(Map<String, WorkflowStep> byName) {
        this.stepByType = byName != null ? new HashMap<>(byName) : new HashMap<>();
    }

    public WorkflowStep get(String name) {
        return stepByType.get(name);
    }

    public Map<String, WorkflowStep> getAll() {
        return Collections.unmodifiableMap(stepByType);
    }
}
