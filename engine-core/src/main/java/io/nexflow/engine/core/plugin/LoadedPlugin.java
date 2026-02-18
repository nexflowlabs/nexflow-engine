package io.nexflow.engine.core.plugin;

import java.util.Map;

/**
 * A plugin loaded by PluginLoader: step name -> handler (or descriptor).
 */
public interface LoadedPlugin {

    String getPluginName();

    String getVersion();

    /** Step name -> step implementation or descriptor. */
    Map<String, ?> getSteps();
}
