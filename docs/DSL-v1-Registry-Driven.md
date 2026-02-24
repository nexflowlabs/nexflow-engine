# v1 Registry-Driven DSL

The engine uses a **registry-driven** workflow model. Workflows are stored in the **database** (PostgreSQL) and loaded via **WorkflowProvider**; the runtime resolves steps by **name** (step type) from the StepRegistry. There is no `"type": "TASK"` or `"type": "END"` in the execution path—steps are plain `WorkflowStep` implementations registered by name.

## Where workflows live

- **Source of truth:** `workflow_definition` and `workflow_step_definition` tables (see [db/queries/v1/schema.sql](../db/queries/v1/schema.sql)).
- **Loading:** The engine uses **WorkflowProvider.getLoadedWorkflow(workflowName)** (backed by **WorkflowLoader** and in-memory cache). Definitions are not loaded from JSON files at runtime.
- **Shape per step:** Each step row has `step_id` (integer), `step_name`, `step_type`, `config_json` (object), and `branches_json` (object: branch key → target `step_id` or `null` for end).

## Structure (conceptual → DB)

Conceptually, a workflow has a **name**, **version**, **start_step_id**, and an ordered set of steps. Each step has:

| Concept   | DB column      | Description |
|----------|-----------------|-------------|
| Step id  | `step_id`       | Integer; used in `branches_json` and as current step pointer. |
| Name     | `step_name`     | Logical name for the step (e.g. `init`, `check_amount`). |
| Type     | `step_type`     | **Registry key.** Engine resolves `StepRegistry.get(step_type)` → `WorkflowStep`. |
| Config   | `config_json`   | JSON object passed to the step as `StepExecutionContext.getStepConfig()`. |
| Branches | `branches_json` | Map: branch key (e.g. `"TRUE"`, `"success"`) → next `step_id` or `null` (workflow complete). |

Reference JSON shape (for mapping to DB or docs):

```json
{
  "version": "1.0",
  "name": "hello-workflow",
  "description": "Optional",
  "steps": [
    {
      "step_id": 1,
      "step_name": "say_hello",
      "step_type": "transform",
      "config": { "newField": "greeting", "value": "Hello" },
      "branches": { "success": 2 }
    },
    {
      "step_id": 2,
      "step_name": "done",
      "step_type": "transform",
      "config": { "newField": "status", "value": "COMPLETED" },
      "branches": { "success": null }
    }
  ]
}
```

- **steps**: In DB, one row per step in `workflow_step_definition`. `workflow_definition.start_step_id` is the first step’s `step_id`.
- **step_id**: Integer; used in `branches_json` and to advance execution.
- **step_type**: Registry key. Engine gets `WorkflowStep` by this name and runs `step.execute(context)`.
- **config**: Stored as `config_json`; passed to the step. Can include **retry** (see below).
- **branches**: Stored as `branches_json`. For `StepResult.branch("KEY")`, engine goes to `branches.get("KEY")` (step_id or null).

## Retry (per step)

Retry is configured **in the step’s config** (e.g. inside `config_json`):

- **retry.maxAttempts**: Max runs for this step (default 1). Example: `3` = first run + 2 retries.
- **retry.backoffSeconds**: Seconds to wait before the next attempt (default 0).

Example:

```json
{
  "retry": { "maxAttempts": 3, "backoffSeconds": 2 },
  "method": "GET",
  "url": "https://api.example.com/call"
}
```

The step receives the current attempt (0-based) via `context.getRetryCount()`. When attempts are exhausted, the workflow is marked FAILED. See [Built-in-Steps.md](Built-in-Steps.md#retry-all-built-in-steps).

## Execution

1. Engine gets **WorkflowView** from WorkflowProvider by workflow name.
2. Sets current step to `start_step_id`, then loads step def by `step_id`.
3. Resolves **WorkflowStep** by def’s `step_type` from StepRegistry.
4. Builds **StepExecutionContext** (executionId, workflowName, tenantId, retryCount, inputs, **stepConfig**).
5. Runs `workflowStep.execute(context)` → **StepResult**.
6. **SUCCESS**: Merges `result.getOutputs()` into context (entries with key `outcome` are **not** merged into the next step’s context; they stay in that step’s output only). Then goes to `branches.get("success")` or single success branch if defined, else workflow completes.
7. **BRANCH**: Goes to `branches.get(result.getBranchName())` (or `def.next` if present); `null` = workflow complete.
8. **FAILURE**: Mark execution FAILED.
9. **RETRY**: Same step re-run if retry config allows and attempts not exhausted; otherwise FAILED.

No type enum, no switch on type. Built-in and custom steps are both `WorkflowStep` beans registered by name.

## API (start and resume)

- **Start:** `POST /api/v1/workflows/{workflowName}/start` with header `Idempotency-Key`. Returns `executionId` and `status: STARTED` immediately; steps run on a virtual thread.
- **Resume:** `POST /api/v1/workflows/resume/{waitToken}` with optional body (e.g. `outcome` for branching). Returns `202 Accepted`; resume runs asynchronously.

See [API.md](API.md).

## Built-in steps (by name / step_type)

| step_type  | config example | notes |
|------------|----------------|-------|
| wait       | `durationMs`    | Synchronous sleep (dev). |
| condition  | `field`, `operator`, `value` | Returns branch TRUE/FALSE. Use `branches`. |
| transform  | `newField`, `value` or key-value | Merges into context. |
| expression | `expression`   | Evaluates expression; branches e.g. true/false. |
| script     | `language`, `code`, optional `timeoutMs` | JavaScript (Rhino); call `success()`, `failure()`, or `branch()`. |
| httpCall   | `method`, `url`, `headers`, `body` | Java HttpClient. |
| emitEvent  | `eventType`    | Uses EmitEventPublisher if set. |
| aiDecision | `endpoint` or `promptKey`, `confidenceThreshold` | Returns branch HIGH/LOW. |

Custom steps are registered as beans implementing `WorkflowStep` and override `getName()` to match the step type used in the DB.
