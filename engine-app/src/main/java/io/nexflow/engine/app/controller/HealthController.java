package io.nexflow.engine.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "Liveness check")
public class HealthController {

    @Operation(summary = "Health check")
    @GetMapping("/health")
    public String health() {
        return "Nexflow Engine running";
    }
}
