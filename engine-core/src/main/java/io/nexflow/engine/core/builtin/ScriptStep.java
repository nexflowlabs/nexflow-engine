package io.nexflow.engine.core.builtin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexflow.sdk.core.StepExecutionContext;
import com.nexflow.sdk.core.StepResult;
import com.nexflow.sdk.core.WorkflowStep;
import io.nexflow.engine.core.script.ScriptExecutionClient;
import io.nexflow.engine.core.script.ScriptExecutionResult;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.nexflow.engine.core.builtin.StepConstants.OUTCOME_KEY;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Built-in step: JavaScript execution (Rhino; runs on any JDK). Config: language, mode (IN_PROCESS | ISOLATED), code, timeoutMs (optional, default 200).
 * Script must return via helper functions: success(outputs), failure(), branch(name).
 * In-process: sandboxed JS with only inputs, stepConfig, and helpers (no Java/IO/reflection).
 */
public final class ScriptStep implements WorkflowStep {

    private static final String TYPE_SCRIPT = "script";
    private static final String CONFIG_MODE = "mode";
    private static final String CONFIG_CODE = "code";
    private static final String CONFIG_LANGUAGE = "language";
    private static final String CONFIG_TIMEOUT_MS = "timeoutMs";
    private static final String MODE_IN_PROCESS = "IN_PROCESS";
    private static final String MODE_ISOLATED = "ISOLATED";
    private static final String DEFAULT_LANGUAGE = "javascript";
    private static final String SCRIPT_RESULT_STATUS = "status";
    private static final String SCRIPT_RESULT_OUTPUTS = "outputs";
    private static final String SCRIPT_RESULT_BRANCH = "branch";
    private static final String SCRIPT_STATUS_SUCCESS = "SUCCESS";
    private static final String SCRIPT_STATUS_FAILURE = "FAILURE";
    private static final String SCRIPT_STATUS_BRANCH = "BRANCH";
    private static final String SCRIPT_BINDING_RESULT = "__result";
    private static final String THREAD_NAME_SCRIPT = "nexflow-script";
    private static final String LOG_PREFIX = "script step: ";
    private static final String ERROR_REQUIRES_CONFIG = "script step requires config";
    private static final String ERROR_REQUIRES_CODE = "script step requires config.code";
    private static final String ERROR_INVALID_INPUTS = "script step: invalid inputs/config";
    private static final String ERROR_TIMEOUT = "script step: execution timeout";
    private static final String ERROR_INTERRUPTED = "script step: interrupted";
    private static final String ERROR_MUST_CALL_HELPERS = "script must call success(outputs), failure(), or branch(name)";
    private static final String ERROR_INVALID_RESULT = "invalid script result";
    private static final String ERROR_BRANCH_BLANK = "script branch() must provide a non-blank name";
    private static final String ERROR_MUST_RETURN_STATUS = "script must return SUCCESS, FAILURE, or BRANCH";

    private static final Logger LOG = Logger.getLogger(ScriptStep.class.getName());
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long DEFAULT_TIMEOUT_MS = 200L;

    private final ScriptExecutionClient isolatedClient;

    public ScriptStep() {
        this(null);
    }

    public ScriptStep(ScriptExecutionClient isolatedClient) {
        this.isolatedClient = isolatedClient;
    }

    @Override
    public String getType() {
        return TYPE_SCRIPT;
    }

    @Override
    public StepResult execute(StepExecutionContext context) {
        Map<String, Object> config = context.getStepConfig();
        if (config == null) {
            return StepResult.failure(ERROR_REQUIRES_CONFIG);
        }
        String mode = config.containsKey(CONFIG_MODE) ? String.valueOf(config.get(CONFIG_MODE)) : MODE_IN_PROCESS;
        String code = config.get(CONFIG_CODE) != null ? String.valueOf(config.get(CONFIG_CODE)) : null;
        if (code == null || code.isBlank()) {
            LOG.log(Level.WARNING, LOG_PREFIX + "missing or empty config.code");
            return StepResult.failure(ERROR_REQUIRES_CODE);
        }
        long timeoutMs = timeoutFromConfig(config);
        String language = config.get(CONFIG_LANGUAGE) != null ? String.valueOf(config.get(CONFIG_LANGUAGE)) : DEFAULT_LANGUAGE;

        if (MODE_ISOLATED.equalsIgnoreCase(mode) && isolatedClient != null) {
            ScriptExecutionResult result = isolatedClient.execute(language, code, context.getInputs(), config, timeoutMs);
            return mapToStepResult(result);
        }

        return runInProcess(code, context.getInputs(), config, timeoutMs);
    }

    private static long timeoutFromConfig(Map<String, Object> config) {
        Object t = config.get(CONFIG_TIMEOUT_MS);
        if (t == null) return DEFAULT_TIMEOUT_MS;
        if (t instanceof Number n) return Math.max(1, n.longValue());
        try {
            return Math.max(1, Long.parseLong(String.valueOf(t)));
        } catch (NumberFormatException e) {
            return DEFAULT_TIMEOUT_MS;
        }
    }

    private static StepResult mapToStepResult(ScriptExecutionResult r) {
        switch (r.getStatus()) {
            case SUCCESS: {
                Map<String, Object> outputs = r.getOutputs() != null ? new HashMap<>(r.getOutputs()) : new HashMap<>();
                if (!outputs.containsKey(OUTCOME_KEY)) {
                    outputs.put(OUTCOME_KEY, "success");
                }
                return StepResult.success(outputs);
            }
            case FAILURE:
                return StepResult.failure(r.getErrorMessage());
            case BRANCH: {
                String branch = r.getBranchName();
                Map<String, Object> outputs = new HashMap<>();
                outputs.put(OUTCOME_KEY, branch);
                return StepResult.branch(branch, outputs);
            }
            default:
                return StepResult.failure();
        }
    }

    private static StepResult runInProcess(String code, Map<String, Object> inputs, Map<String, Object> stepConfig, long timeoutMs) {
        String inputsJson;
        String stepConfigJson;
        try {
            inputsJson = JSON.writeValueAsString(inputs != null ? inputs : Map.of());
            stepConfigJson = JSON.writeValueAsString(stepConfig != null ? stepConfig : Map.of());
        } catch (JsonProcessingException e) {
            LOG.log(Level.WARNING, LOG_PREFIX + "failed to serialize inputs/config: " + e.getMessage());
            return StepResult.failure(ERROR_INVALID_INPUTS);
        }
        String escapedInputs = escapeForJsString(inputsJson);
        String escapedConfig = escapeForJsString(stepConfigJson);

        String preamble = "var " + SCRIPT_BINDING_RESULT + " = null; "
                + "function success(o) { " + SCRIPT_BINDING_RESULT + " = { status: '" + SCRIPT_STATUS_SUCCESS + "', " + SCRIPT_RESULT_OUTPUTS + ": o != null ? o : {} }; } "
                + "function failure() { " + SCRIPT_BINDING_RESULT + " = { status: '" + SCRIPT_STATUS_FAILURE + "' }; } "
                + "function branch(n) { " + SCRIPT_BINDING_RESULT + " = { status: '" + SCRIPT_STATUS_BRANCH + "', " + SCRIPT_RESULT_BRANCH + ": n }; } "
                + "var inputs = JSON.parse(" + escapedInputs + "); "
                + "var stepConfig = JSON.parse(" + escapedConfig + "); ";
        String fullScript = preamble + code;

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, THREAD_NAME_SCRIPT);
            t.setDaemon(true);
            return t;
        });
        try {
            Future<Scriptable> future = executor.submit(() -> {
                Context cx = Context.enter();
                try {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    Scriptable scope = cx.initStandardObjects();
                    cx.evaluateString(scope, fullScript, "nexflow-script", 1, null);
                    Object result = scope.get(SCRIPT_BINDING_RESULT, scope);
                    return (result instanceof Scriptable) ? (Scriptable) result : null;
                } finally {
                    Context.exit();
                }
            });
            Scriptable __result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return readScriptResult(__result);
        } catch (TimeoutException e) {
            LOG.log(Level.WARNING, LOG_PREFIX + "execution exceeded timeout " + timeoutMs + "ms");
            return StepResult.failure(ERROR_TIMEOUT);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            LOG.log(Level.WARNING, LOG_PREFIX + "execution error: " + (cause != null ? cause.getMessage() : e.getMessage()));
            return StepResult.failure(cause != null ? cause.getMessage() : "script execution failed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return StepResult.failure(ERROR_INTERRUPTED);
        } finally {
            executor.shutdownNow();
        }
    }

    private static String escapeForJsString(String json) {
        StringBuilder sb = new StringBuilder(json.length() + 2);
        sb.append('"');
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') sb.append("\\\\");
            else if (c == '"') sb.append("\\\"");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else sb.append(c);
        }
        sb.append('"');
        return sb.toString();
    }

    private static StepResult readScriptResult(Scriptable __result) {
        try {
            if (__result == null) {
                LOG.log(Level.WARNING, LOG_PREFIX + "script did not call success(), failure(), or branch()");
                return StepResult.failure(ERROR_MUST_CALL_HELPERS);
            }
            Object statusObj = __result.get(SCRIPT_RESULT_STATUS, __result);
            String status = (statusObj != null && statusObj != Context.getUndefinedValue()) ? Context.toString(statusObj) : null;
            if (status == null) return StepResult.failure(ERROR_INVALID_RESULT);
            switch (status) {
                case SCRIPT_STATUS_SUCCESS: {
                    Object outputsObj = __result.get(SCRIPT_RESULT_OUTPUTS, __result);
                    Map<String, Object> outputs = (outputsObj instanceof Scriptable)
                            ? scriptableToMap((Scriptable) outputsObj) : Collections.emptyMap();
                    Map<String, Object> mutable = new HashMap<>(outputs);
                    if (!mutable.containsKey(OUTCOME_KEY)) {
                        mutable.put(OUTCOME_KEY, "success");
                    }
                    return StepResult.success(mutable);
                }
                case SCRIPT_STATUS_FAILURE:
                    return StepResult.failure();
                case SCRIPT_STATUS_BRANCH: {
                    Object branchObj = __result.get(SCRIPT_RESULT_BRANCH, __result);
                    String branch = (branchObj != null && branchObj != Context.getUndefinedValue()) ? Context.toString(branchObj) : null;
                    if (branch == null || branch.isBlank()) {
                        return StepResult.failure(ERROR_BRANCH_BLANK);
                    }
                    return StepResult.branch(branch, Map.of(OUTCOME_KEY, branch));
                }
                default:
                    return StepResult.failure(ERROR_MUST_RETURN_STATUS);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, LOG_PREFIX + "failed to read result: " + e.getMessage());
            return StepResult.failure(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> scriptableToMap(Scriptable s) {
        try {
            Map<String, Object> out = new HashMap<>();
            for (Object id : s.getIds()) {
                String key = id instanceof String ? (String) id : String.valueOf(id);
                Object val = s.get(key, s);
                if (val == null || val == Context.getUndefinedValue()) {
                    out.put(key, null);
                } else if (val instanceof Number) {
                    out.put(key, ((Number) val).doubleValue());
                } else if (val instanceof String || val instanceof Boolean) {
                    out.put(key, val);
                } else if (val instanceof Scriptable) {
                    Scriptable nested = (Scriptable) val;
                    if (nested.getClassName().equals("Object") || nested.getClassName().equals("Array")) {
                        out.put(key, scriptableToMap(nested));
                    } else {
                        out.put(key, Context.toString(val));
                    }
                } else {
                    out.put(key, Context.toString(val));
                }
            }
            return out;
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
