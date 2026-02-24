package io.nexflow.engine.core.runtime;

import io.nexflow.engine.core.definition.StepDefinition;
import io.nexflow.engine.core.step.AiDecisionEvaluator;
import io.nexflow.engine.core.step.AiDecisionStep;

import java.util.Map;

public class StepExecutor {

    private AiDecisionEvaluator aiDecisionEvaluator;

    public void setAiDecisionEvaluator(AiDecisionEvaluator aiDecisionEvaluator) {
        this.aiDecisionEvaluator = aiDecisionEvaluator;
    }

    public TransitionResult execute(StepDefinition step, ExecutionContext ctx) {
        String type = step.getType();
        if (type == null) type = "";
        return switch (type) {
            case "TASK" -> TransitionResult.waitForTask();
            case "DECISION" -> {
                boolean result = false; // stub
                Map<String, Integer> branches = step.getBranches();
                String next = result && branches != null && branches.containsKey("TRUE")
                        ? String.valueOf(branches.get("TRUE"))
                        : (branches != null && branches.containsKey("FALSE") ? String.valueOf(branches.get("FALSE")) : null);
                yield next != null ? TransitionResult.continueTo(next) : TransitionResult.complete();
            }
            case "WAIT" -> TransitionResult.waitForTask();
            case "AI_DECISION", "aiDecision" -> AiDecisionStep.execute(step, ctx, aiDecisionEvaluator);
            case "END" -> TransitionResult.complete();
            default -> TransitionResult.complete();
        };
    }
}
