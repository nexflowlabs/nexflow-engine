package io.nexflow.engine.core.builtin;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import com.nexflow.sdk.core.WorkflowStep;

import java.util.Map;

import static io.nexflow.engine.core.builtin.StepConstants.OUTCOME_KEY;

/**
 * Built-in step: wait for a duration (synchronous). Config: durationMs.
 */
public final class WaitStep implements WorkflowStep {

    private static final String TYPE_WAIT = "wait";
    private static final String CONFIG_DURATION_MS = "durationMs";
    private static final String ERROR_INTERRUPTED = "interrupted";

    @Override
    public String getType() {
        return TYPE_WAIT;
    }

    @Override
    public StepResult execute(StepExecutionContext context) {
        Map<String, Object> config = context.getStepConfig();
        Object v = config.getOrDefault(CONFIG_DURATION_MS, 0);
        long durationMs = v instanceof Number ? ((Number) v).longValue() : 0;
        try {
            if (durationMs > 0) {
                Thread.sleep(durationMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return StepResult.failure(ERROR_INTERRUPTED);
        }
        return StepResult.success(Map.of(OUTCOME_KEY, "success"));
    }
}
