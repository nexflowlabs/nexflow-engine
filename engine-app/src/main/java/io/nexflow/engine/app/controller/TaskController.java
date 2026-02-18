package io.nexflow.engine.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexflow.engine.app.runtime.WorkflowRuntimeService;
import io.nexflow.engine.persistence.entity.TaskExecutionEntity;
import io.nexflow.engine.persistence.repository.TaskExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskExecutionRepository taskRepo;
//    private final WorkflowCommandService commandService;
    private final WorkflowRuntimeService runtimeService;
    private final ObjectMapper objectMapper;

    @GetMapping("/poll")
    public TaskExecutionEntity poll() {
        return taskRepo
                .findFirstByStatusOrderByCreatedAtAsc("PENDING")
                .map(task -> {
                    task.setStatus("RUNNING");
                    task.setUpdatedAt(Instant.now());
                    return taskRepo.save(task);
                })
                .orElse(null);
    }

    @PostMapping("/{taskId}/complete")
    public void complete(
            @PathVariable UUID taskId,
            @RequestBody Map<String, Object> output
    ) throws Exception {

        TaskExecutionEntity task =
                taskRepo.findById(taskId).orElseThrow();

        task.setStatus("COMPLETED");
        task.setOutputJson(objectMapper.writeValueAsString(output));
        task.setUpdatedAt(Instant.now());
        taskRepo.save(task);

        runtimeService.resume(task); // ✅ VALID, NO CYCLE
    }

    @PostMapping("/{taskId}/fail")
    public void failTask(
            @PathVariable UUID taskId,
            @RequestBody Map<String, Object> error
    ) throws Exception {

        TaskExecutionEntity task =
                taskRepo.findById(taskId).orElseThrow();

        task.setStatus("FAILED");
        task.setOutputJson(objectMapper.writeValueAsString(error));
        task.setUpdatedAt(Instant.now());
        taskRepo.save(task);

        runtimeService.handleTaskFailure(task);
    }


}

