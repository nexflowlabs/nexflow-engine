package io.nexflow.engine.app.controller;

import io.nexflow.engine.app.runtime.WorkflowRuntimeService;
import io.nexflow.engine.persistence.entity.WaitExecutionEntity;
import io.nexflow.engine.persistence.repository.WaitExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final WaitExecutionRepository waitRepo;
    private final WorkflowRuntimeService runtimeService;

    @PostMapping("/{eventName}")
    public void publishEvent(@PathVariable String eventName) throws Exception {

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
