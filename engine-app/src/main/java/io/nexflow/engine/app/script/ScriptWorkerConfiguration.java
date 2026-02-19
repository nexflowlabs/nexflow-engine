package io.nexflow.engine.app.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexflow.engine.core.script.ScriptExecutionClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(ScriptWorkerProperties.class)
public class ScriptWorkerConfiguration {

    @ConditionalOnProperty(name = "nexflow.script.worker-endpoint")
    @Bean
    public ScriptExecutionClient httpScriptExecutionClient(
            ScriptWorkerProperties properties,
            RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        return new HttpScriptExecutionClient(
                properties.getWorkerEndpoint(),
                restTemplate,
                objectMapper
        );
    }
}
