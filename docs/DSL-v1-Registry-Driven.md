# v1 Registry-Driven DSL

The engine supports a **registry-driven** workflow DSL. There is no `"type": "TASK"` or `"type": "END"`. Steps are resolved by **name** from the StepRegistry and executed as `WorkflowStep`.

## Structure

```json
{
  "version": "1.0",
  "name": "hello-workflow",
  "description": "Optional",
  "trigger": { "type": "MANUAL" },
  "steps": [
    {
      "id": "say_hello",
      "name": "helloTask",
      "config": {},
      "next": "end"
    },
    {
      "id": "end",
      "name": "transform",
      "config": { "newField": "status", "value": "COMPLETED" }
    }
  ]
}
```

- **steps**: Array of steps. Order defines execution start (first step is the entry).
- **id**: Step identifier; used in `next` and `branches`.
- **name**: Registry key. Engine resolves `StepRegistry.get(name)` → `WorkflowStep` and runs `step.execute(context)`.
- **config**: Passed to the step as `StepExecutionContext.getStepConfig()`.
- **next**: Id of the next step after success. Omit or null = workflow completes.
- **branches**: Optional. For steps that return `StepResult.branch("KEY")`, map branch key → step id (e.g. `"TRUE": "highValue", "FALSE": "lowValue"`).

## Execution

1. Engine sets current step to the first step’s **id**.
2. Resolves step def by id, then `WorkflowStep` by def **name** from StepRegistry.
3. Builds `StepExecutionContext` (executionId, workflowName, tenantId, retryCount, inputs, **stepConfig**).
4. Runs `workflowStep.execute(context)` → `StepResult`.
5. **SUCCESS**: Merge `result.getOutputs()` into context; go to `def.next` (or complete if null).
6. **BRANCH**: Go to `def.branches.get(result.getBranchName())` (or `def.next` if missing).
7. **FAILURE**: Mark execution FAILED.
8. **RETRY**: Re-run same step (v1 simple retry).

No type enum, no switch on type. Built-in and custom steps are both plain `WorkflowStep` implementations registered by name.

## Built-in steps (by name)

| name        | config example | notes |
|------------|----------------|-------|
| wait       | `durationMs`   | Synchronous sleep (dev). |
| condition  | `field`, `operator`, `value` | Returns branch TRUE/FALSE. Use `branches`. |
| transform  | `newField`, `value` or key-value | Merges into context. |
| httpCall   | `method`, `url`, `headers`, `body` | Java HttpClient. |
| emitEvent  | `eventType`    | Uses EmitEventPublisher if set. |
| aiDecision | `endpoint` or `promptKey`, `confidenceThreshold` | Returns branch HIGH/LOW. |

Custom steps (e.g. `helloTask`) are registered as beans implementing SDK `WorkflowStep` and override `getName()` to match the DSL step name.
