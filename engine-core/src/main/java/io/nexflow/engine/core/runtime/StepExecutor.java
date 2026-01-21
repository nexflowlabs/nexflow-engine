package io.nexflow.engine.core.runtime;

import io.nexflow.engine.core.definition.StepDefinition;

public class StepExecutor {

    public TransitionResult execute(StepDefinition step, ExecutionContext ctx) {

        return switch (step.getType()) {

            case TASK -> TransitionResult.waitForTask();

            case DECISION -> {
                boolean result = false; // stub
                yield TransitionResult.continueTo(
                        result ? step.getOnTrue() : step.getOnFalse()
                );
            }

            case WAIT -> TransitionResult.waitForTask();

            case END -> TransitionResult.complete();
        };
    }
}
