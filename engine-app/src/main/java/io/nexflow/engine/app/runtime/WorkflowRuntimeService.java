package io.nexflow.engine.app.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexflow.engine.app.loader.WorkflowProvider;
import io.nexflow.engine.app.loader.WorkflowView;
import io.nexflow.engine.core.builtin.StepConstants;
import io.nexflow.engine.core.definition.StepDefinition;
import io.nexflow.engine.core.definition.WorkflowDefinition;
import io.nexflow.engine.core.runtime.ExecutionContext;
import io.nexflow.engine.core.runtime.RegistryDrivenRuntime;
import io.nexflow.engine.core.runtime.StepOutcome;
import io.nexflow.engine.persistence.entity.*;
import io.nexflow.engine.persistence.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

@Service
@Transactional
public class WorkflowRuntimeService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowRuntimeService.class);

    private final RegistryDrivenRuntime registryDrivenRuntime;
    private final WorkflowExecutionRepository executionRepo;
    private final WorkflowDefinitionRepository definitionRepo;
    private final IdempotencyKeyRepository idemRepo;
    private final WaitExecutionRepository waitRepo;
    private final StepExecutionRepository stepExecutionRepo;
    private final WorkflowProvider workflowProvider;
    private final ObjectMapper objectMapper;
    private final ExecutorService workflowExecutor;

    public WorkflowRuntimeService(
            RegistryDrivenRuntime registryDrivenRuntime,
            WorkflowExecutionRepository executionRepo,
            WorkflowDefinitionRepository definitionRepo,
            IdempotencyKeyRepository idemRepo,
            WaitExecutionRepository waitRepo,
            StepExecutionRepository stepExecutionRepo,
            WorkflowProvider workflowProvider,
            ObjectMapper objectMapper,
            @Qualifier("workflowExecutor") ExecutorService workflowExecutor) {
        this.registryDrivenRuntime = registryDrivenRuntime;
        this.executionRepo = executionRepo;
        this.definitionRepo = definitionRepo;
        this.idemRepo = idemRepo;
        this.waitRepo = waitRepo;
        this.stepExecutionRepo = stepExecutionRepo;
        this.workflowProvider = workflowProvider;
        this.objectMapper = objectMapper;
        this.workflowExecutor = workflowExecutor;
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

        WorkflowView loaded = workflowProvider.getLoadedWorkflow(def.getId(), true).orElse(null);
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

        idemRepo.save(
                IdempotencyKeyEntity.builder()
                        .scope("WORKFLOW_START")
                        .key(idempotencyKey)
                        .referenceId(exec.getId())
                        .createdAt(Instant.now())
                        .build()
        );

        // Run workflow steps on a virtual thread; return immediately with executionId and STARTED
        final Long executionId = exec.getId();
        workflowExecutor.submit(() -> {
            try {
                advanceAfterStart(executionId);
            } catch (Exception e) {
                log.error("Workflow advance failed for executionId={}", executionId, e);
            }
        });

        return exec.getId();
    }

    /**
     * Runs in a virtual thread: loads execution and workflow, then advances until complete/fail/wait.
     * Called asynchronously after start; uses its own transaction.
     */
    @Transactional
    public void advanceAfterStart(Long executionId) throws Exception {
        WorkflowExecutionEntity exec = executionRepo.findById(executionId)
                .orElseThrow(() -> new IllegalStateException("Execution not found: " + executionId));
        WorkflowView loaded = workflowProvider.getLoadedWorkflow(exec.getWorkflowDefinitionId(), true).orElse(null);
        if (loaded == null) {
            throw new IllegalStateException("Workflow definition or steps not found for execution: " + executionId);
        }
        advance(exec, loaded.getDefinition());
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
        StepDefinition stepDef = workflow.getStepById(currentStepId);
        RetryConfig retry = parseRetryConfig(stepDef != null ? stepDef.getConfig() : null);

        int attempt = (int) stepExecutionRepo.countByExecutionIdAndStepId(exec.getId(), currentStepId) + 1;
        if (attempt > retry.maxAttempts) {
            exec.setStatus("FAILED");
            exec.setCurrentStepId(null);
            Map<String, Object> data = objectMapper.readValue(exec.getContextJson(), Map.class);
            data.put("errorMessage", "Max retries exceeded for step " + currentStepId + " (maxAttempts=" + retry.maxAttempts + ")");
            exec.setContextJson(objectMapper.writeValueAsString(data));
            exec.setUpdatedAt(Instant.now());
            executionRepo.save(exec);
            return false;
        }

        Map<String, Object> data = objectMapper.readValue(exec.getContextJson(), Map.class);
        ExecutionContext context = new ExecutionContext(exec.getId().toString(), data);
        String workflowName = workflow.getName() != null ? workflow.getName() : "";

        // 1. Record step start in step_execution
        Instant stepStartedAt = Instant.now();
        StepExecutionEntity stepExec = new StepExecutionEntity();
        stepExec.setExecutionId(exec.getId());
        stepExec.setStepId(currentStepId);
        stepExec.setStatus("STARTED");
        stepExec.setAttempt(attempt);
        stepExec.setInputJson(exec.getContextJson());
        stepExec.setStartedAt(stepStartedAt);
        stepExecutionRepo.save(stepExec);

        StepOutcome outcome = registryDrivenRuntime.runStep(
                workflow,
                currentStepId,
                context,
                workflowName,
                null,
                attempt - 1
        );

        // 2. Record step end: status, endedAt, output
        Instant stepEndedAt = Instant.now();
        stepExec.setEndedAt(stepEndedAt);
        stepExec.setStatus(outcomeStatusToStepStatus(outcome.getType()));
        stepExec.setOutputJson(outcome.getContextUpdates() != null && !outcome.getContextUpdates().isEmpty()
                ? objectMapper.writeValueAsString(outcome.getContextUpdates())
                : null);
        stepExecutionRepo.save(stepExec);

        // Merge step outputs into execution context for next step, but exclude "outcome" so it does not travel across steps (each step's outcome is only in its own output_json).
        if (outcome.getContextUpdates() != null && !outcome.getContextUpdates().isEmpty()) {
            for (Map.Entry<String, Object> e : outcome.getContextUpdates().entrySet()) {
                if (!StepConstants.OUTCOME_KEY.equals(e.getKey())) {
                    data.put(e.getKey(), e.getValue());
                }
            }
            exec.setContextJson(objectMapper.writeValueAsString(data));
        }
        exec.setUpdatedAt(Instant.now());

        switch (outcome.getType()) {
            case CONTINUE -> {
                Integer nextId = outcome.getNextStepId();
                if (nextId != null && nextId.equals(currentStepId)) {
                    // Step returned RETRY: same step again
                    if (attempt >= retry.maxAttempts) {
                        exec.setStatus("FAILED");
                        exec.setCurrentStepId(null);
                        data.put("errorMessage", "Max retries exceeded for step " + currentStepId + " (maxAttempts=" + retry.maxAttempts + ")");
                        exec.setContextJson(objectMapper.writeValueAsString(data));
                        exec.setUpdatedAt(Instant.now());
                        executionRepo.save(exec);
                        return false;
                    }
                    if (retry.backoffSeconds > 0) {
                        try {
                            Thread.sleep(retry.backoffSeconds * 1000L);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Retry backoff interrupted", e);
                        }
                    }
                    exec.setCurrentStepId(currentStepId);
                    executionRepo.save(exec);
                    return true;
                }
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

    /** Retry settings for a step. Read from step config "retry": { "maxAttempts": N, "backoffSeconds": S }. */
    private static final class RetryConfig {
        static final int DEFAULT_MAX_ATTEMPTS = 1;
        static final int DEFAULT_BACKOFF_SECONDS = 0;
        final int maxAttempts;
        final int backoffSeconds;

        RetryConfig(int maxAttempts, int backoffSeconds) {
            this.maxAttempts = Math.max(1, maxAttempts);
            this.backoffSeconds = Math.max(0, backoffSeconds);
        }
    }

    private static RetryConfig parseRetryConfig(Map<String, Object> stepConfig) {
        if (stepConfig == null) return new RetryConfig(RetryConfig.DEFAULT_MAX_ATTEMPTS, RetryConfig.DEFAULT_BACKOFF_SECONDS);
        Object retryObj = stepConfig.get("retry");
        if (!(retryObj instanceof Map<?, ?> retryMap)) return new RetryConfig(RetryConfig.DEFAULT_MAX_ATTEMPTS, RetryConfig.DEFAULT_BACKOFF_SECONDS);
        int max = RetryConfig.DEFAULT_MAX_ATTEMPTS;
        int backoff = RetryConfig.DEFAULT_BACKOFF_SECONDS;
        Object m = retryMap.get("maxAttempts");
        if (m instanceof Number n) max = n.intValue();
        Object b = retryMap.get("backoffSeconds");
        if (b instanceof Number n) backoff = n.intValue();
        return new RetryConfig(max, backoff);
    }

    /** Resume a workflow execution after a wait (TIME or EVENT) completed. Loads graph by workflow_definition_id. */
    public void resumeWait(WaitExecutionEntity wait) throws Exception {
        resumeWaitWithContext(wait, null);
    }

    /**
     * Resumes a workflow by wait token (e.g. from webhook/callback). Looks up the wait in the service,
     * submits resume to a virtual thread, and returns whether a matching wait was found.
     * No repository logic in controller; call this from the workflow controller only.
     *
     * @param waitToken   token from StepResult.waitForEvent(waitToken, ...)
     * @param contextMerge optional payload to merge into execution context (e.g. outcome for branching)
     * @return true if a WAITING wait was found and resume was submitted, false otherwise (404)
     */
    public boolean resumeByToken(String waitToken, Map<String, Object> contextMerge) {
        if (waitToken == null || waitToken.isBlank()) {
            return false;
        }
        return waitRepo.findByWaitTokenAndStatus(waitToken, "WAITING")
                .map(wait -> {
                    submitResumeAsync(wait.getId(), contextMerge != null ? contextMerge : Map.of());
                    return true;
                })
                .orElse(false);
    }

    /**
     * Submits resume to a virtual thread and returns immediately.
     * Used internally after wait is resolved by token or id.
     */
    public void submitResumeAsync(Long waitId, Map<String, Object> contextMerge) {
        workflowExecutor.submit(() -> {
            try {
                runResume(waitId, contextMerge != null ? contextMerge : Map.of());
            } catch (Exception e) {
                log.error("Webhook resume failed for waitId={}", waitId, e);
            }
        });
    }

    /**
     * Runs in a virtual thread: loads wait by id and resumes the workflow.
     * Uses its own transaction.
     */
    @Transactional
    public void runResume(Long waitId, Map<String, Object> contextMerge) throws Exception {
        WaitExecutionEntity wait = waitRepo.findById(waitId)
                .orElseThrow(() -> new IllegalStateException("Wait not found: " + waitId));
        resumeWaitWithContext(wait, contextMerge);
    }

    /** Resume and optionally merge webhook payload into execution context before advancing. */
    public void resumeWaitWithContext(WaitExecutionEntity wait, Map<String, Object> contextMerge) throws Exception {
        WorkflowExecutionEntity exec = executionRepo.findById(wait.getExecutionId()).orElseThrow();
        WorkflowDefinition workflow = workflowProvider.getLoadedWorkflow(exec.getWorkflowDefinitionId(), false)
                .map(WorkflowView::getDefinition)
                .orElseThrow(() -> new IllegalStateException("Workflow definition or steps not found for execution: " + exec.getId()));
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
