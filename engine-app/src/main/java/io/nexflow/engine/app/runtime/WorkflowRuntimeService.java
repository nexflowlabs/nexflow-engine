package io.nexflow.engine.app.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexflow.engine.app.loader.NormalizedWorkflowLoader;
import io.nexflow.engine.core.builtin.StepConstants;
import io.nexflow.engine.core.definition.WorkflowDefinition;
import io.nexflow.engine.core.runtime.ExecutionContext;
import io.nexflow.engine.core.runtime.RegistryDrivenRuntime;
import io.nexflow.engine.core.runtime.StepOutcome;
import io.nexflow.engine.persistence.entity.*;
import io.nexflow.engine.persistence.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class WorkflowRuntimeService {

    private final RegistryDrivenRuntime registryDrivenRuntime;
    private final WorkflowExecutionRepository executionRepo;
    private final WorkflowDefinitionRepository definitionRepo;
    private final IdempotencyKeyRepository idemRepo;
    private final WaitExecutionRepository waitRepo;
    private final StepExecutionRepository stepExecutionRepo;
    private final NormalizedWorkflowLoader workflowLoader;
    private final ObjectMapper objectMapper;

    public WorkflowRuntimeService(
            RegistryDrivenRuntime registryDrivenRuntime,
            WorkflowExecutionRepository executionRepo,
            WorkflowDefinitionRepository definitionRepo,
            IdempotencyKeyRepository idemRepo,
            WaitExecutionRepository waitRepo,
            StepExecutionRepository stepExecutionRepo,
            NormalizedWorkflowLoader workflowLoader,
            ObjectMapper objectMapper) {
        this.registryDrivenRuntime = registryDrivenRuntime;
        this.executionRepo = executionRepo;
        this.definitionRepo = definitionRepo;
        this.idemRepo = idemRepo;
        this.waitRepo = waitRepo;
        this.stepExecutionRepo = stepExecutionRepo;
        this.workflowLoader = workflowLoader;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Long startIdempotent(
            String workflowName,
            String idempotencyKey,
            Map<String, Object> input
    ) throws Exception {

        Optional<IdempotencyKeyEntity> existing =
                idemRepo.findByScopeAndKey("WORKFLOW_START", idempotencyKey);

        if (existing.isPresent()) {
            return existing.get().getReferenceId();
        }

        WorkflowDefinitionEntity def = definitionRepo.findActivePublishedByName(workflowName);
        if (def == null) {
            throw new IllegalStateException("No active published workflow found for name: " + workflowName);
        }

        NormalizedWorkflowLoader.LoadedWorkflow loaded = workflowLoader.load(def.getId(), true);
        if (loaded == null || loaded.getStartStepId() == null) {
            throw new IllegalStateException("Workflow definition has no steps or start step: " + workflowName);
        }

        WorkflowExecutionEntity exec = WorkflowExecutionEntity.builder()
                .workflowDefinitionId(def.getId())
                .status("RUNNING")
                .currentStepId(loaded.getStartStepId())
                .contextJson(objectMapper.writeValueAsString(input != null ? input : Map.of()))
                .startedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        executionRepo.save(exec);
        advance(exec, loaded.getDefinition());

        idemRepo.save(
                IdempotencyKeyEntity.builder()
                        .scope("WORKFLOW_START")
                        .key(idempotencyKey)
                        .referenceId(exec.getId())
                        .createdAt(Instant.now())
                        .build()
        );

        return exec.getId();
    }

    /**
     * Advances the execution using the in-memory workflow graph. Runs steps in a loop until
     * the workflow completes, fails, or has no current step (no recursion).
     */
    private void advance(WorkflowExecutionEntity exec, WorkflowDefinition workflow) throws Exception {
        while (true) {
            if (exec.getCurrentStepId() == null) {
                exec.setStatus("COMPLETED");
                exec.setUpdatedAt(Instant.now());
                executionRepo.save(exec);
                return;
            }
            boolean shouldContinue = executeNextStep(exec, workflow);
            if (!shouldContinue) {
                return;
            }
        }
    }

    /**
     * Executes the current step once, updates execution state and persists. Returns true
     * if the workflow should advance to the next step (CONTINUE), false if terminal (COMPLETE/FAIL).
     */
    private boolean executeNextStep(WorkflowExecutionEntity exec, WorkflowDefinition workflow) throws Exception {
        Integer currentStepId = exec.getCurrentStepId();
        Map<String, Object> data = objectMapper.readValue(exec.getContextJson(), Map.class);
        ExecutionContext context = new ExecutionContext(exec.getId().toString(), data);
        String workflowName = workflow.getName() != null ? workflow.getName() : "";

        // 1. Record step start in step_execution
        Instant stepStartedAt = Instant.now();
        StepExecutionEntity stepExec = new StepExecutionEntity();
        stepExec.setExecutionId(exec.getId());
        stepExec.setStepId(currentStepId);
        stepExec.setStatus("STARTED");
        stepExec.setAttempt(1);
        stepExec.setInputJson(exec.getContextJson());
        stepExec.setStartedAt(stepStartedAt);
        stepExecutionRepo.save(stepExec);

        StepOutcome outcome = registryDrivenRuntime.runStep(
                workflow,
                currentStepId,
                context,
                workflowName,
                null,
                0
        );

        // 2. Record step end: status, endedAt, output
        Instant stepEndedAt = Instant.now();
        stepExec.setEndedAt(stepEndedAt);
        stepExec.setStatus(outcomeStatusToStepStatus(outcome.getType()));
        stepExec.setOutputJson(outcome.getContextUpdates() != null && !outcome.getContextUpdates().isEmpty()
                ? objectMapper.writeValueAsString(outcome.getContextUpdates())
                : null);
        stepExecutionRepo.save(stepExec);

        if (outcome.getContextUpdates() != null && !outcome.getContextUpdates().isEmpty()) {
            data.putAll(outcome.getContextUpdates());
            exec.setContextJson(objectMapper.writeValueAsString(data));
        }
        exec.setUpdatedAt(Instant.now());

        switch (outcome.getType()) {
            case CONTINUE -> {
                Integer nextId = outcome.getNextStepId();
                exec.setCurrentStepId(nextId);
                executionRepo.save(exec);
                return true;
            }
            case COMPLETE -> {
                exec.setStatus("COMPLETED");
                exec.setCurrentStepId(null);
                executionRepo.save(exec);
                return false;
            }
            case FAIL -> {
                exec.setStatus("FAILED");
                exec.setCurrentStepId(null);
                if (outcome.getErrorMessage() != null && !outcome.getErrorMessage().isBlank()) {
                    data.put("errorMessage", outcome.getErrorMessage());
                    exec.setContextJson(objectMapper.writeValueAsString(data));
                }
                executionRepo.save(exec);
                return false;
            }
            case WAIT -> {
                exec.setStatus("WAITING");
                executionRepo.save(exec);
                WaitExecutionEntity waitEntity = new WaitExecutionEntity();
                waitEntity.setExecutionId(exec.getId());
                waitEntity.setStepId(currentStepId);
                waitEntity.setWaitType("EVENT");
                waitEntity.setEventName("webhook");
                waitEntity.setWaitToken(outcome.getWaitToken());
                waitEntity.setNextStepIdWhenResumed(null);
                waitEntity.setStatus("WAITING");
                waitEntity.setCreatedAt(Instant.now());
                waitEntity.setUpdatedAt(Instant.now());
                waitRepo.save(waitEntity);
                return false;
            }
        }
        return false;
    }

    private static String outcomeStatusToStepStatus(StepOutcome.Type type) {
        return switch (type) {
            case CONTINUE, COMPLETE -> "COMPLETED";
            case FAIL -> "FAILED";
            case WAIT -> "WAITING";
        };
    }

    private static Integer parseStepId(String nextStepId) {
        if (nextStepId == null || nextStepId.isBlank()) return null;
        try {
            return Integer.parseInt(nextStepId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Resume a workflow execution after a wait (TIME or EVENT) completed. Loads graph by workflow_definition_id. */
    public void resumeWait(WaitExecutionEntity wait) throws Exception {
        resumeWaitWithContext(wait, null);
    }

    /** Resume and optionally merge webhook payload into execution context before advancing. */
    public void resumeWaitWithContext(WaitExecutionEntity wait, Map<String, Object> contextMerge) throws Exception {
        WorkflowExecutionEntity exec = executionRepo.findById(wait.getExecutionId()).orElseThrow();
        WorkflowDefinition workflow;
        {
            NormalizedWorkflowLoader.LoadedWorkflow loaded = workflowLoader.load(exec.getWorkflowDefinitionId(), false);
            if (loaded == null) {
                throw new IllegalStateException("Workflow definition or steps not found for execution: " + exec.getId());
            }
            workflow = loaded.getDefinition();
        }
        // 1. Keep input data in contextJson (merge webhook payload into context)
        if (contextMerge != null && !contextMerge.isEmpty()) {
            Map<String, Object> data = objectMapper.readValue(exec.getContextJson(), Map.class);
            data.putAll(contextMerge);
            exec.setContextJson(objectMapper.writeValueAsString(data));
            exec.setUpdatedAt(Instant.now());
        }
        // 2. Mark wait as completed
        wait.setStatus("COMPLETED");
        wait.setUpdatedAt(Instant.now());
        waitRepo.save(wait);
        // 3. Resolve next step from outcome in payload (required for dynamic branching)
        String branchName = null;
        if (contextMerge != null && contextMerge.containsKey(StepConstants.OUTCOME_KEY)) {
            Object o = contextMerge.get(StepConstants.OUTCOME_KEY);
            if (o != null) branchName = o.toString().trim();
        }
        if (branchName == null || branchName.isEmpty()) {
            exec.setStatus("FAILED");
            exec.setCurrentStepId(null);
            Map<String, Object> data = objectMapper.readValue(exec.getContextJson(), Map.class);
            data.put("errorMessage", "outcome is required in webhook payload (response must include '" + StepConstants.OUTCOME_KEY + "')");
            exec.setContextJson(objectMapper.writeValueAsString(data));
            exec.setUpdatedAt(Instant.now());
            executionRepo.save(exec);
            return;
        }
        Integer nextStepId;
        try {
            nextStepId = RegistryDrivenRuntime.resolveNextStepId(workflow, wait.getStepId(), branchName);
        } catch (IllegalArgumentException e) {
            exec.setStatus("FAILED");
            exec.setCurrentStepId(null);
            Map<String, Object> data = objectMapper.readValue(exec.getContextJson(), Map.class);
            data.put("errorMessage", e.getMessage());
            exec.setContextJson(objectMapper.writeValueAsString(data));
            exec.setUpdatedAt(Instant.now());
            executionRepo.save(exec);
            return;
        }
        exec.setCurrentStepId(nextStepId);
        exec.setStatus(nextStepId != null ? "RUNNING" : "COMPLETED");
        exec.setUpdatedAt(Instant.now());
        executionRepo.save(exec);
        if (nextStepId != null) {
            advance(exec, workflow);
        }
    }
}
