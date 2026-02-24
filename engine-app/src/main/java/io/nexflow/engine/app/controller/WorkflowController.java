package io.nexflow.engine.app.controller;

import io.nexflow.engine.app.runtime.WorkflowRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Workflow start and resume. Tenant is resolved from X-Tenant-Id header (or default in single-tenant mode).
 * When adding GET endpoints that return workflow or execution by id, call
 * {@link io.nexflow.engine.tenant.TenantVerification#ensureTenantMatch} before returning.
 */
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflows", description = "Start and resume workflow executions")
public class WorkflowController {

    private final WorkflowRuntimeService runtimeService;

    @Operation(summary = "Start workflow", description = "Start a workflow by name. Requires Idempotency-Key header. Optional JSON body is the initial input.")
    @PostMapping("/{workflowName}/start")
    public ResponseEntity<Map<String, Object>> startWorkflow(
            @PathVariable("workflowName") String workflowName,
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

    @Operation(summary = "Resume workflow", description = "Resume a workflow waiting for an event (by wait token). Optional JSON body is merged into execution context; include 'outcome' for branching.")
    @PostMapping("/resume/{waitToken}")
    public ResponseEntity<Void> resumeByToken(
            @PathVariable("waitToken") String waitToken,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        if (waitToken == null || waitToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean accepted = runtimeService.resumeByToken(waitToken, body != null ? body : Map.of());
        return accepted
                ? ResponseEntity.status(HttpStatus.ACCEPTED).build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
