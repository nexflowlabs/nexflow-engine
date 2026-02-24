package io.nexflow.engine.core.builtin;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import com.nexflow.sdk.core.WorkflowStep;

import java.util.HashMap;
import java.util.Map;

import static io.nexflow.engine.core.builtin.StepConstants.OUTCOME_KEY;

/**
 * Built-in step: add/replace context with config values. Config: newField, value (or key-value pairs).
 */
public final class TransformStep implements WorkflowStep {

    private static final String TYPE_TRANSFORM = "transform";
    private static final String CONFIG_NEW_FIELD = "newField";
    private static final String CONFIG_VALUE = "value";

    @Override
    public String getType() {
        return TYPE_TRANSFORM;
    }

    @Override
    public StepResult execute(StepExecutionContext context) {
        Map<String, Object> config = context.getStepConfig();
        Map<String, Object> outputs = new HashMap<>();
        Object newField = config.get(CONFIG_NEW_FIELD);
        Object value = config.get(CONFIG_VALUE);
        if (newField != null && value != null) {
            outputs.put(String.valueOf(newField), value);
        }
        for (Map.Entry<String, Object> e : config.entrySet()) {
            if (!CONFIG_NEW_FIELD.equals(e.getKey()) && !CONFIG_VALUE.equals(e.getKey())) {
                outputs.put(e.getKey(), e.getValue());
            }
        }
        outputs.put(OUTCOME_KEY, "success");
        return StepResult.success(outputs);
    }
}
