package io.nexflow.engine.core.plugin;

import java.util.List;

/**
 * Loads step implementations from plugins (e.g. file-based or classpath).
 * Engine-app provides the implementation; built-in steps are registered separately via BuiltInStepConfiguration.
 */
public interface PluginLoader {

    /**
     * Load and return plugin descriptors or step handlers from the configured plugin source.
     */
    List<LoadedPlugin> load();
}
