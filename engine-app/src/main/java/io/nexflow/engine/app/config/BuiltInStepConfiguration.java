package io.nexflow.engine.app.config;

import io.nexflow.engine.core.definition.StepType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Declares which step types are built-in (handled inside engine-core).
 * Plugin-loaded steps are separate and registered via PluginLoader.
 */
@Configuration
public class BuiltInStepConfiguration {

    /** Built-in step types: implemented in engine-core, no plugin required. */
    public static final Set<StepType> BUILT_IN_STEP_TYPES = Set.of(
            StepType.TASK,
            StepType.DECISION,
            StepType.WAIT,
            StepType.END,
            StepType.AI_DECISION
    );

    @Bean
    public Set<StepType> builtInStepTypes() {
        return BUILT_IN_STEP_TYPES;
    }
}
