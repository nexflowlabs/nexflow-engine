package io.nexflow.engine.app.loader;

import io.nexflow.engine.core.definition.WorkflowDefinition;

/**
 * Read-only view of a loaded workflow: definition graph and start step id.
 * Returned by {@link WorkflowProvider}; hides loader internals.
 */
public final class WorkflowView {

    private final WorkflowDefinition definition;
    private final Integer startStepId;

    public WorkflowView(WorkflowDefinition definition, Integer startStepId) {
        this.definition = definition;
        this.startStepId = startStepId;
    }

    public WorkflowDefinition getDefinition() {
        return definition;
    }

    public Integer getStartStepId() {
        return startStepId;
    }
}
