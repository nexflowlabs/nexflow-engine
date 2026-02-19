package io.nexflow.engine.app.controller;

import io.nexflow.engine.app.runtime.WorkflowRuntimeService;
import io.nexflow.engine.persistence.repository.WaitExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook endpoint to resume a workflow that is waiting for an external event.
 * Lookup is by wait token (from StepResult.waitForEvent(waitToken, ...)).
 */
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WaitExecutionRepository waitRepo;
    private final WorkflowRuntimeService runtimeService;

    /**
     * Resume a workflow that is waiting for an event, by its wait token.
     * Optional request body (JSON) is merged into the execution context before resuming.
     *
     * @param waitToken the token returned when the step called StepResult.waitForEvent(waitToken, ...)
     * @param body      optional JSON map to merge into execution context (e.g. webhook payload)
     */
    @PostMapping("/resume/{waitToken}")
    public ResponseEntity<Void> resumeByToken(
            @PathVariable String waitToken,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        if (waitToken == null || waitToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return waitRepo.findByWaitTokenAndStatus(waitToken, "WAITING")
                .map(wait -> {
                    try {
                        runtimeService.resumeWaitWithContext(wait, body != null ? body : Map.of());
                        return ResponseEntity.ok().<Void>build();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
