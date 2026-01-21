package io.nexflow.sdk.client;

import io.nexflow.sdk.model.Task;

import java.util.Map;
import java.util.UUID;

public interface NexflowClient {
    Task poll();
    void complete(UUID taskId, Map<String, Object> output);
}

