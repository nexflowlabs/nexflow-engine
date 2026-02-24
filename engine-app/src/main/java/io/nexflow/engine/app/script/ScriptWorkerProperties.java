package io.nexflow.engine.app.script;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for script isolated worker. When worker-endpoint is set, script step can use mode ISOLATED.
 */
@ConfigurationProperties(prefix = "nexflow.script")
public class ScriptWorkerProperties {

    /**
     * HTTP endpoint for the script execution worker (e.g. http://localhost:9090/execute).
     * If not set, script step falls back to IN_PROCESS only.
     */
    private String workerEndpoint;

    public String getWorkerEndpoint() {
        return workerEndpoint;
    }

    public void setWorkerEndpoint(String workerEndpoint) {
        this.workerEndpoint = workerEndpoint;
    }
}
