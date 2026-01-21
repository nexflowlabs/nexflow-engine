package io.nexflow.engine.app.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexflow.engine.core.definition.RetryPolicy;
import io.nexflow.engine.core.definition.StepDefinition;
import io.nexflow.engine.core.definition.StepType;
import io.nexflow.engine.core.definition.WorkflowDefinition;
import io.nexflow.engine.core.runtime.ExecutionContext;
import io.nexflow.engine.core.runtime.TransitionResult;
import io.nexflow.engine.core.runtime.WorkflowRuntime;
import io.nexflow.engine.persistence.entity.TaskExecutionEntity;
import io.nexflow.engine.persistence.entity.WaitExecutionEntity;
import io.nexflow.engine.persistence.entity.WorkflowDefinitionEntity;
import io.nexflow.engine.persistence.entity.WorkflowExecutionEntity;
import io.nexflow.engine.persistence.repository.TaskExecutionRepository;
import io.nexflow.engine.persistence.repository.WaitExecutionRepository;
import io.nexflow.engine.persistence.repository.WorkflowDefinitionRepository;
import io.nexflow.engine.persistence.repository.WorkflowExecutionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowRuntimeService {

    private final WorkflowRuntime runtime;
    private final WorkflowExecutionRepository executionRepo;
    private final WorkflowDefinitionRepository definitionRepo;
    private final TaskExecutionRepository taskRepo;
    private final WaitExecutionRepository waitRepo;
    private final ObjectMapper objectMapper;

    /* ---------------- START ---------------- */

    public void startExecution(
            WorkflowDefinitionEntity defEntity,
            Map<String, Object> input
    ) throws Exception {

        WorkflowDefinition workflow =
                objectMapper.readValue(defEntity.getDefinitionJson(), WorkflowDefinition.class);

        WorkflowExecutionEntity exec = WorkflowExecutionEntity.builder()
                .id(UUID.randomUUID())
                .workflowName(defEntity.getName())
                .workflowVersion(defEntity.getVersion())
                .status("RUNNING")
                .currentStep(workflow.getStart())
                .contextJson(objectMapper.writeValueAsString(input))
                .startedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        executionRepo.save(exec);
        advance(exec, workflow);
    }

    /* ---------------- RESUME ---------------- */

    public void resume(TaskExecutionEntity task) throws Exception {

        WorkflowExecutionEntity exec =
                executionRepo.findById(task.getExecutionId()).orElseThrow();

        WorkflowDefinitionEntity defEntity =
                definitionRepo.findByNameAndVersion(
                        exec.getWorkflowName(),
                        exec.getWorkflowVersion()
                );

        WorkflowDefinition workflow =
                objectMapper.readValue(defEntity.getDefinitionJson(), WorkflowDefinition.class);

        Map<String, Object> ctx =
                objectMapper.readValue(exec.getContextJson(), Map.class);

        // step name is the KEY, not inside StepDefinition
        ctx.put(task.getStepName(),
                objectMapper.readValue(task.getOutputJson(), Map.class));

        exec.setContextJson(objectMapper.writeValueAsString(ctx));
        exec.setCurrentStep(
                workflow.getSteps()
                        .get(task.getStepName())
                        .getOnSuccess()
        );
        exec.setUpdatedAt(Instant.now());
        executionRepo.save(exec);

        advance(exec, workflow);
    }

    /* ---------------- CORE ADVANCE ---------------- */

    private void advance(
            WorkflowExecutionEntity exec,
            WorkflowDefinition workflow
    ) throws Exception {

        ExecutionContext context = new ExecutionContext(
                exec.getId().toString(),
                objectMapper.readValue(exec.getContextJson(), Map.class)
        );

        String stepName = exec.getCurrentStep();
        StepDefinition step = workflow.getSteps().get(stepName);

        TransitionResult result =
                runtime.runStep(workflow, stepName, context);

        switch (result.getType()) {

            case CONTINUE -> {
                exec.setCurrentStep(result.getNextStep());
                persist(exec, context);
                advance(exec, workflow);
            }

            case WAIT -> {
                if (step.getType() == StepType.TASK) {
                    createTask(exec, step, context);
                } else if (step.getType() == StepType.WAIT) {
                    createWait(exec, step);
                }
            }

            case COMPLETE -> {
                exec.setStatus("COMPLETED");
                exec.setCurrentStep(null);
                persist(exec, context);
            }
        }
    }

    /* ---------------- TASK CREATION ---------------- */

    private void createTask(
            WorkflowExecutionEntity exec,
            StepDefinition step,
            ExecutionContext context
    ) throws Exception {

        TaskExecutionEntity task = TaskExecutionEntity.builder()
                .id(UUID.randomUUID())
                .executionId(exec.getId())
                .stepName(exec.getCurrentStep())
                .taskName(step.getTask())
                .status("PENDING")
                .attempt(1)
                .inputJson(objectMapper.writeValueAsString(context.getData()))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        taskRepo.save(task);
    }

    /* ---------------- PERSIST ---------------- */

    private void persist(
            WorkflowExecutionEntity exec,
            ExecutionContext context
    ) throws Exception {

        exec.setContextJson(objectMapper.writeValueAsString(context.getData()));
        exec.setUpdatedAt(Instant.now());
        executionRepo.save(exec);
    }

    @Transactional
    public void handleTaskFailure(TaskExecutionEntity task) throws Exception {

        WorkflowExecutionEntity exec =
                executionRepo.findById(task.getExecutionId()).orElseThrow();

        WorkflowDefinitionEntity defEntity =
                definitionRepo.findByNameAndVersion(
                        exec.getWorkflowName(),
                        exec.getWorkflowVersion()
                );

        WorkflowDefinition workflow =
                objectMapper.readValue(defEntity.getDefinitionJson(), WorkflowDefinition.class);

        StepDefinition step =
                workflow.getSteps().get(task.getStepName());

        RetryPolicy retry = step.getRetry();

        if (retry != null && task.getAttempt() < retry.getMaxAttempts()) {
            retryTask(task, retry);
            return;
        }

        // Retries exhausted → step failure
        handleStepFailure(exec, workflow, step, task);
    }

    private void retryTask(
            TaskExecutionEntity task,
            RetryPolicy retry
    ) {

        task.setAttempt(task.getAttempt() + 1);
        task.setStatus("PENDING");

        // naive backoff (v1)
        task.setUpdatedAt(
                Instant.now().plusSeconds(retry.getBackoffSeconds())
        );

        taskRepo.save(task);
    }

    private void handleStepFailure(
            WorkflowExecutionEntity exec,
            WorkflowDefinition workflow,
            StepDefinition step,
            TaskExecutionEntity task
    ) throws Exception {

        if (step.getOnFailure() == null) {
            // Terminal failure
            exec.setStatus("FAILED");
            exec.setCurrentStep(null);
            exec.setUpdatedAt(Instant.now());
            executionRepo.save(exec);
            return;
        }

        // Move workflow to failure step
        exec.setCurrentStep(step.getOnFailure());
        exec.setUpdatedAt(Instant.now());
        executionRepo.save(exec);

        advance(exec, workflow);
    }

    private void createWait(
            WorkflowExecutionEntity exec,
            StepDefinition step
    ) {

        WaitExecutionEntity wait = WaitExecutionEntity.builder()
                .id(UUID.randomUUID())
                .executionId(exec.getId())
                .stepName(exec.getCurrentStep())
                .waitType(step.getDuration() != null ? "TIME" : "EVENT")
                .waitUntil(
                        step.getDuration() != null
                                ? Instant.now().plus(Duration.parse(step.getDuration()))
                                : null
                )
                .eventName(step.getEvent())
                .status("WAITING")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        waitRepo.save(wait);
    }

    @Transactional
    public void resumeWait(WaitExecutionEntity wait) throws Exception {

        WorkflowExecutionEntity exec =
                executionRepo.findById(wait.getExecutionId()).orElseThrow();

        WorkflowDefinitionEntity defEntity =
                definitionRepo.findByNameAndVersion(
                        exec.getWorkflowName(),
                        exec.getWorkflowVersion()
                );

        WorkflowDefinition workflow =
                objectMapper.readValue(defEntity.getDefinitionJson(), WorkflowDefinition.class);

        StepDefinition step =
                workflow.getSteps().get(wait.getStepName());

        exec.setCurrentStep(step.getNext());
        exec.setUpdatedAt(Instant.now());
        executionRepo.save(exec);

        advance(exec, workflow);
    }

}
