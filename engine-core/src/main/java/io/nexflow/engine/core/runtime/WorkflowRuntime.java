package io.nexflow.engine.core.runtime;

import io.nexflow.engine.core.definition.WorkflowDefinition;
import io.nexflow.engine.core.definition.StepDefinition;

public class WorkflowRuntime {

    private final StepExecutor stepExecutor = new StepExecutor();

    public TransitionResult runStep(
            WorkflowDefinition workflow,
            String stepName,
            ExecutionContext context
    ) {

        StepDefinition step = workflow.getSteps().get(stepName);

        if (step == null) {
            throw new IllegalStateException("Step not found: " + stepName);
        }

        return stepExecutor.execute(step, context);
    }
}

