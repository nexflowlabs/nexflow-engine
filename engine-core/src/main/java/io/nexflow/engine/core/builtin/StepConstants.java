package io.nexflow.engine.core.builtin;

/**
 * Shared string constants for built-in step config keys and payload keys.
 * Step-specific constants remain in each step class.
 */
public final class StepConstants {

    private StepConstants() { }

    /** Required key in step result outputs and webhook payload for branch resolution. Value = branch name. */
    public static final String OUTCOME_KEY = "outcome";

    // ----- Wait / callback config (EmitEventStep, HttpCallStep, etc.) -----
    /** Config: wait for external callback before continuing (boolean). */
    public static final String CONFIG_WAIT_FOR_CALLBACK = "waitForCallback";
    /** Config: prefix for generated wait token (executionId and UUID are appended). */
    public static final String CONFIG_WAIT_TOKEN = "waitToken";
    /** Config: HTTP header name to send wait token in (e.g. X-Callback-Token). */
    public static final String CONFIG_WAIT_TOKEN_HEADER = "waitTokenHeader";

    /** Payload key: wait token included in emitted event for callback correlation. */
    public static final String PAYLOAD_WAIT_TOKEN = "waitToken";

    /** Boolean string for config parsing. */
    public static final String BOOLEAN_TRUE = "true";
}
