package io.nexflow.engine.app.controller;

import io.nexflow.engine.app.runtime.WorkflowRuntimeService;
import io.nexflow.engine.persistence.entity.WaitExecutionEntity;
import io.nexflow.engine.persistence.repository.WaitExecutionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Publish events to resume EVENT-type waits")
public class EventController {

    private final WaitExecutionRepository waitRepo;
    private final WorkflowRuntimeService runtimeService;

    @Operation(summary = "Publish event", description = "Resume all workflows waiting for this event name (EVENT wait type).")
    @PostMapping("/{eventName}")
    public void publishEvent(@PathVariable("eventName") String eventName) throws Exception {

        List<WaitExecutionEntity> waits =
                waitRepo.findByStatusAndWaitTypeAndEventName(
                        "WAITING",
                        "EVENT",
                        eventName
                );

        for (WaitExecutionEntity wait : waits) {
            wait.setStatus("COMPLETED");
            wait.setUpdatedAt(Instant.now());
            waitRepo.save(wait);
            runtimeService.resumeWait(wait);
        }
    }
}
