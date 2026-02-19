package io.nexflow.engine.core.runtime;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import com.nexflow.sdk.core.WorkflowStep;
import io.nexflow.engine.core.builtin.StepConstants;
import io.nexflow.engine.core.definition.StepDefinition;
import io.nexflow.engine.core.definition.WorkflowDefinition;
import io.nexflow.engine.core.registry.StepRegistry;

import java.util.List;
import java.util.Map;

/**
 * Registry-driven execution: resolve step by name, execute WorkflowStep, interpret StepResult.
 * No StepType or switch-case.
 */
public class RegistryDrivenRuntime {

    private final StepRegistry registry;

    public RegistryDrivenRuntime(StepRegistry registry) {
        this.registry = registry;
    }

    /**
     * Run one step by step id. Returns outcome and context updates to merge.
     */
    public StepOutcome runStep(
            WorkflowDefinition workflow,
            Integer currentStepId,
            ExecutionContext context,
            String workflowName,
            String tenantId,
            int retryCount
    ) {
        StepDefinition stepDef = workflow.getStepById(currentStepId);
        if (stepDef == null) {
            throw new IllegalStateException("Step not found: " + currentStepId);
        }
        String stepType = stepDef.getType();
        if (stepType == null || stepType.isBlank()) {
            throw new IllegalStateException("Step " + currentStepId + " has no name");
        }
        WorkflowStep step = registry.get(stepType);
        if (step == null) {
            throw new IllegalStateException("No step registered for name: " + stepType);
        }
        Map<String, Object> config = stepDef.getConfig() != null ? stepDef.getConfig() : Map.of();
        StepExecutionContext stepExecutionContext = new StepExecutionContext(
                context.getExecutionId(),
                workflowName,
                tenantId != null ? tenantId : "",
                retryCount,
                context.getData(),
                config
        );
        StepResult result = step.execute(stepExecutionContext);

        switch (result.getStatus()) {
            case SUCCESS, BRANCH -> {
                Map<String, Object> outputs = result.getOutputs() != null ? result.getOutputs() : Map.of();
                Object outcomeObj = outputs.get(StepConstants.OUTCOME_KEY);
                String outcome = outcomeObj != null ? outcomeObj.toString().trim() : null;
                if (outcome == null || outcome.isEmpty()) {
                    return StepOutcome.fail("outcome is required in step result (response must include '" + StepConstants.OUTCOME_KEY + "')");
                }
                return resolveBranchOutcome(stepDef, outcome, outputs);
            }
            case FAILURE -> {
                return StepOutcome.fail();
            }
            case RETRY -> {
                return StepOutcome.continueTo(currentStepId, Map.of());
            }
            case WAIT -> {
                String token = result.getWaitToken();
                Map<String, Object> outputs = result.getOutputs() != null ? result.getOutputs() : Map.of();
                return StepOutcome.waitForEvent(token, outputs);
            }
            default -> {
                return StepOutcome.fail();
            }
        }
    }

    /**
     * Resolve outcome from branches only: unknown branch -> FAIL with message;
     * branch present with null target -> COMPLETE; else CONTINUE to target.
     */
    private static StepOutcome resolveBranchOutcome(StepDefinition stepDef, String branchName, Map<String, Object> outputs) {
        Map<String, Integer> branches = stepDef.getBranches();
        if (branches == null ) {
            return StepOutcome.complete(outputs);
        }
        if (!branches.containsKey(branchName)) {
            return StepOutcome.fail("Unknown branch: " + branchName);
        }
        Integer target = branches.get(branchName);
        if (target == null) {
            return StepOutcome.complete(outputs);
        }
        return StepOutcome.continueTo(target, outputs);
    }

    /**
     * Resolve next step id from branches only. branchName required.
     * @return target step id, or null if branch target is null (workflow complete)
     * @throws IllegalArgumentException if branch name missing or unknown
     */
    private static Integer resolveBranchNext(StepDefinition stepDef, String branchName) {
        if (branchName == null || branchName.isBlank()) {
            throw new IllegalArgumentException("Branch name required");
        }
        Map<String, Integer> branches = stepDef.getBranches();
        if (branches == null || !branches.containsKey(branchName)) {
            throw new IllegalArgumentException("Unknown branch: " + branchName);
        }
        return branches.get(branchName);
    }

    /**
     * Returns the first step id for this workflow (steps[0].id), or null if empty.
     */
    public static Integer getStartStepId(WorkflowDefinition workflow) {
        List<StepDefinition> steps = workflow.getSteps();
        if (steps == null || steps.isEmpty()) return null;
        return steps.get(0).getId();
    }

    /**
     * Resolve the next step id after a step (e.g. when resuming from WAIT).
     * Branch name is required; uses branches only. Returns null if branch target is null (workflow complete).
     * @throws IllegalArgumentException if branch name missing or unknown
     */
    public static Integer resolveNextStepId(WorkflowDefinition workflow, Integer currentStepId, String branchName) {
        StepDefinition stepDef = workflow.getStepById(currentStepId);
        if (stepDef == null) return null;
        return resolveBranchNext(stepDef, branchName);
    }
}
