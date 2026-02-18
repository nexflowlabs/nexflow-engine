# Built-in Steps

These step types are implemented in engine-core and do not require a plugin.

| Step type   | Description |
|------------|-------------|
| **task**   | Dispatches work to an external worker; then `onSuccess` or `onFailure`. Supports `retry`. |
| **decision** | Branches on an expression; `onTrue` / `onFalse`. |
| **wait**   | Pauses for a duration (ISO-8601) or an event; then `next`. |
| **end**    | Terminates the workflow; optional `status`. |
| **aiDecision** | Calls the configured AI evaluator; branches on confidence threshold to `onHighConfidence` / `onLowConfidence`. |

## aiDecision

- **promptKey**: Passed to the AI evaluator (e.g. prompt or intent key).
- **confidenceThreshold**: Number in [0, 1]. If evaluator returns ≥ threshold, go to `onHighConfidence`; else `onLowConfidence`.
- **onHighConfidence** / **onLowConfidence**: Next step names (required).

The engine requires an `AiDecisionEvaluator` bean when any workflow uses `aiDecision`. Set `nexflow.ai-decision.url` to use the HTTP evaluator (e.g. mock-ai-server).

## Registration

Built-in steps are declared in `BuiltInStepConfiguration`. Plugin-loaded steps are separate and loaded via `PluginLoader`.
