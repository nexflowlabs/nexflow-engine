package io.nexflow.engine.app.step;

import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import com.nexflow.sdk.core.WorkflowStep;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Example custom step registered by name "helloTask". Used by sample v1 workflow.
 */
@Component
public class HelloTaskStep implements WorkflowStep {

    @Override
    public String getType() {
        return "helloTask";
    }

    @Override
    public StepResult execute(StepExecutionContext context) {
        return StepResult.success(Map.of("message", "Hello Nexflow!"));
    }
}
