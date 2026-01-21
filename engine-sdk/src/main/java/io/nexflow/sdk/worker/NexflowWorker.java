
package io.nexflow.sdk.worker;

import java.util.Map;

public interface NexflowWorker {
    String taskName();
    Map<String, Object> execute(Map<String, Object> input);
}

