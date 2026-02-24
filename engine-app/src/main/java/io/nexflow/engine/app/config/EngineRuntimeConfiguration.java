package io.nexflow.engine.app.config;

import io.nexflow.engine.app.ai.HttpAiDecisionEvaluator;
import io.nexflow.engine.core.registry.StepRegistry;
import io.nexflow.engine.core.runtime.RegistryDrivenRuntime;
import io.nexflow.engine.core.step.AiDecisionEvaluator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wires registry-driven runtime, workflow async executor, and optional AI evaluator.
 */
@Configuration
public class EngineRuntimeConfiguration {

    /** Virtual-thread executor for running workflow advance and webhook resume off the request thread. */
    @Bean(name = "workflowExecutor")
    public ExecutorService workflowExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    public RegistryDrivenRuntime registryDrivenRuntime(StepRegistry stepRegistry) {
        return new RegistryDrivenRuntime(stepRegistry);
    }

    @ConditionalOnProperty(name = "nexflow.ai-decision.url")
    @Bean
    public AiDecisionEvaluator httpAiDecisionEvaluator(
            @Value("${nexflow.ai-decision.url}") String baseUrl
    ) {
        return new HttpAiDecisionEvaluator(RestClient.builder().baseUrl(baseUrl).build());
    }
}
