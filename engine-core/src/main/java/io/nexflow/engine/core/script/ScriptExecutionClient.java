package io.nexflow.engine.core.script;

import java.util.Map;

/**
 * Client for executing scripts in an isolated worker (e.g. HTTP).
 * When script step mode is ISOLATED and this client is configured, the engine delegates execution here.
 */
public interface ScriptExecutionClient {

    /**
     * Execute script in the isolated worker.
     *
     * @param language   e.g. "javascript"
     * @param code       script source
     * @param inputs     execution inputs
     * @param stepConfig step config map
     * @param timeoutMs  max execution time in milliseconds
     * @return structured result (SUCCESS, FAILURE, or BRANCH only)
     */
    ScriptExecutionResult execute(String language, String code, Map<String, Object> inputs,
                                  Map<String, Object> stepConfig, long timeoutMs);
}
