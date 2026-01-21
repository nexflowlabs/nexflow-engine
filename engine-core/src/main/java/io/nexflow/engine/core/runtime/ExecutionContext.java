package io.nexflow.engine.core.runtime;

import java.util.Map;

public class ExecutionContext {

    private final String executionId;
    private final Map<String, Object> data;

    public ExecutionContext(String executionId, Map<String, Object> data) {
        this.executionId = executionId;
        this.data = data;
    }

    public String getExecutionId() {
        return executionId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }
}
