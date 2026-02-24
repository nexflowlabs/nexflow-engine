package io.nexflow.engine.app.config;

import com.nexflow.sdk.core.WorkflowStep;
import io.nexflow.engine.core.builtin.*;
import io.nexflow.engine.core.registry.StepRegistry;
import io.nexflow.engine.core.script.ScriptExecutionClient;
import io.nexflow.engine.core.step.AiDecisionEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registers built-in steps as WorkflowStep beans and builds StepRegistry from all WorkflowStep beans.
 * No type enum; all steps are registry-driven by name.
 */
@Configuration
public class BuiltInStepConfiguration {

    @Bean
    public WaitStep waitStep() {
        return new WaitStep();
    }

    @Bean
    public ConditionStep conditionStep() {
        return new ConditionStep();
    }

    @Bean
    public TransformStep transformStep() {
        return new TransformStep();
    }

    @Bean
    public HttpCallStep httpCallStep() {
        return new HttpCallStep();
    }

    @Bean
    public EmitEventStep emitEventStep() {
        return new EmitEventStep();
    }

    @Bean
    public AiDecisionStepBuiltin aiDecisionStep(Optional<AiDecisionEvaluator> evaluator) {
        return new AiDecisionStepBuiltin(evaluator.orElse(null));
    }

    @Bean
    public ExpressionStep expressionStep() {
        return new ExpressionStep();
    }

    @Bean
    public ScriptStep scriptStep(Optional<ScriptExecutionClient> scriptExecutionClient) {
        return new ScriptStep(scriptExecutionClient.orElse(null));
    }

    @Bean
    public StepRegistry stepRegistry(List<WorkflowStep> steps) {
        Map<String, WorkflowStep> stepByType = new HashMap<>();
        for (WorkflowStep step : steps) {
            String type = step.getType();
            if (type != null && !type.isBlank()) {
                stepByType.put(type, step);
            }
        }
        return new StepRegistry(stepByType);
    }
}
