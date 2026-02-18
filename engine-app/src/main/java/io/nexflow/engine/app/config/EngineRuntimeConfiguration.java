package io.nexflow.engine.app.config;

import io.nexflow.engine.app.ai.HttpAiDecisionEvaluator;
import io.nexflow.engine.core.runtime.WorkflowRuntime;
import io.nexflow.engine.core.step.AiDecisionEvaluator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Wires engine-core runtime and optional AI evaluator.
 */
@Configuration
public class EngineRuntimeConfiguration {

    @Bean
    public WorkflowRuntime workflowRuntime(Optional<AiDecisionEvaluator> aiDecisionEvaluator) {
        WorkflowRuntime runtime = new WorkflowRuntime();
        aiDecisionEvaluator.ifPresent(runtime::setAiDecisionEvaluator);
        return runtime;
    }

    @ConditionalOnProperty(name = "nexflow.ai-decision.url")
    @Bean
    public AiDecisionEvaluator httpAiDecisionEvaluator(
            @Value("${nexflow.ai-decision.url}") String baseUrl
    ) {
        return new HttpAiDecisionEvaluator(RestClient.builder().baseUrl(baseUrl).build());
    }
}
