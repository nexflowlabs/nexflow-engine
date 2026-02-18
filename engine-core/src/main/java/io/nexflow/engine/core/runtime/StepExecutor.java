package io.nexflow.engine.core.runtime;

import io.nexflow.engine.core.definition.StepDefinition;
import io.nexflow.engine.core.definition.StepType;
import io.nexflow.engine.core.step.AiDecisionEvaluator;
import io.nexflow.engine.core.step.AiDecisionStep;

public class StepExecutor {

    private AiDecisionEvaluator aiDecisionEvaluator;

    public void setAiDecisionEvaluator(AiDecisionEvaluator aiDecisionEvaluator) {
        this.aiDecisionEvaluator = aiDecisionEvaluator;
    }

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
            case AI_DECISION -> AiDecisionStep.execute(step, ctx, aiDecisionEvaluator);
        };
    }
}
