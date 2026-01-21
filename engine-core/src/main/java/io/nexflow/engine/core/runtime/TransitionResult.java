package io.nexflow.engine.core.runtime;

public class TransitionResult {

    private final TransitionType type;
    private final String nextStep;

    public enum TransitionType {
        CONTINUE,   // move immediately
        WAIT,  // pause for worker
        COMPLETE
    }

    private TransitionResult(TransitionType type, String nextStep) {
        this.type = type;
        this.nextStep = nextStep;
    }

    public static TransitionResult continueTo(String step) {
        return new TransitionResult(TransitionType.CONTINUE, step);
    }

    public static TransitionResult waitForTask() {
        return new TransitionResult(TransitionType.WAIT, null);
    }

    public static TransitionResult complete() {
        return new TransitionResult(TransitionType.COMPLETE, null);
    }

    public TransitionType getType() { return type; }
    public String getNextStep() { return nextStep; }
}
