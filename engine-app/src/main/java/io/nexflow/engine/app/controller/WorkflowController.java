package io.nexflow.engine.app.controller;

import io.nexflow.engine.app.runtime.WorkflowRuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowRuntimeService runtimeService;

    @PostMapping("/{workflowName}/start")
    public ResponseEntity<Map<String, Object>> startWorkflow(
            @PathVariable String workflowName,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody(required = false) Map<String, Object> input
    ) throws Exception {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Idempotency-Key header is required"));
        }

        Long executionId = runtimeService.startIdempotent(
                workflowName,
                idempotencyKey,
                input != null ? input : Map.of()
        );

        return ResponseEntity.ok(
                Map.of(
                        "executionId", executionId,
                        "status", "STARTED"
                )
        );
    }
}
