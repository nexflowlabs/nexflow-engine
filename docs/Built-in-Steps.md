# Built-in Steps

These step types are implemented in engine-core and do not require a plugin.

| Step type   | Description |
|------------|-------------|
| **task**   | Dispatches work to an external worker; then `onSuccess` or `onFailure`. Supports `retry`. |
| **decision** | Branches on an expression; `onTrue` / `onFalse`. |
| **wait**   | Pauses for a duration (ISO-8601) or an event; then `next`. |
| **end**    | Terminates the workflow; optional `status`. |
| **aiDecision** | Calls the configured AI evaluator; branches on confidence threshold to `onHighConfidence` / `onLowConfidence`. |
| **script** | Runs JavaScript (Rhino) in-process; config `code`, optional `timeoutMs`. Must call `success(outputs)`, `failure()`, or `branch(name)`. Works on any JDK 21 (no GraalVM required). |

## aiDecision

- **promptKey**: Passed to the AI evaluator (e.g. prompt or intent key).
- **confidenceThreshold**: Number in [0, 1]. If evaluator returns ≥ threshold, go to `onHighConfidence`; else `onLowConfidence`.
- **onHighConfidence** / **onLowConfidence**: Next step names (required).

The engine requires an `AiDecisionEvaluator` bean when any workflow uses `aiDecision`. Set `nexflow.ai-decision.url` to use the HTTP evaluator (e.g. mock-ai-server).

## script

- **code**: JavaScript source. Has access to `inputs`, `stepConfig`, and helpers: `success(outputs)`, `failure()`, `branch(name)`.
- **timeoutMs**: Optional; default 200 ms.
- **Engine**: Mozilla Rhino (in-process). Runs on any JDK 21; GraalVM is not required.
- **mode**: Config can specify `IN_PROCESS` (default) or `ISOLATED` when a script worker endpoint is configured.

## Retry (all built-in steps)

Any step can request a retry by returning `StepResult.retry()`. The engine only retries when the step’s **config** includes a `retry` object:

- **retry.maxAttempts**: Maximum number of runs for this step (default 1 = no retry). Example: `3` allows first run plus 2 retries.
- **retry.backoffSeconds**: Seconds to wait before the next attempt (default 0).

Example step config (e.g. in `config_json` for that step):

```json
{
  "retry": { "maxAttempts": 3, "backoffSeconds": 2 },
  "url": "https://api.example.com/call"
}
```

The step sees the current attempt (0-based) via `context.getRetryCount()`. When attempts are exhausted, the workflow is marked FAILED.

## Registration

Built-in steps are declared in `BuiltInStepConfiguration`. Plugin-loaded steps are separate and loaded via `PluginLoader`.
