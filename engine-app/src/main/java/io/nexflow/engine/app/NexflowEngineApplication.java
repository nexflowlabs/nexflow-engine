
package io.nexflow.engine.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "io.nexflow")
@EnableJpaRepositories(basePackages = "io.nexflow.engine.persistence.repository")
@EntityScan(basePackages = "io.nexflow.engine.persistence.entity")
@EnableScheduling
public class NexflowEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexflowEngineApplication.class, args);
    }
}
