package io.nexflow.engine.core.definition;

public class RetryPolicy {

    private int maxAttempts;
    private int backoffSeconds;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getBackoffSeconds() {
        return backoffSeconds;
    }

    public void setBackoffSeconds(int backoffSeconds) {
        this.backoffSeconds = backoffSeconds;
    }
}

