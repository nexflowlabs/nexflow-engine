package io.nexflow.engine.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nexflowEngineOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nexflow Engine API")
                        .description("Workflow execution: start workflows, resume by webhook, publish events.")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Nexflow")));
    }
}
