# Execution Model & Guarantees

## Execution guarantees

### Idempotency

The engine enforces idempotency at these boundaries:

| Boundary        | Scope             | Behavior |
|-----------------|-------------------|----------|
| Workflow start  | `WORKFLOW_START`  | Same idempotency key → returns existing execution ID; no duplicate run. |
| Task completion | `TASK_COMPLETE`  | Duplicate completion for same task is ignored or applied once. |
| Event resume    | `EVENT_RESUME`   | Same event key does not create duplicate resume. |

Idempotency keys are stored in `idempotency_key` (scope, key_value, reference_id). When a duplicate request arrives, the engine returns the stored `reference_id` instead of creating a new execution.

### Async execution (start and resume)

- **Start workflow:** The API validates idempotency, creates the execution and idempotency key, then returns immediately with `executionId` and status `STARTED`. Step execution runs on a **virtual thread** (Java 21); the HTTP request does not block until the workflow completes.
- **Resume by wait token:** The API validates that a WAITING wait exists for the token, then returns `202 Accepted`. Resume and subsequent steps run on a virtual thread. See [API.md](API.md) for `POST /api/v1/workflows/resume/{waitToken}`.

### Context and step outcome

- Step outputs (including the routing **outcome** for branching) are stored in **step_execution.output_json**.
- Only **data** outputs are merged into the execution context for the next step; the **outcome** key is not passed to the next step’s context, so each step’s outcome is local to that step.

### When execution resumes

- **After a TASK step**: Engine creates a task execution (PENDING). When a worker completes the task, the engine resumes the workflow from the step’s `onSuccess` (or `onFailure` if configured).
- **After a WAIT step**: Engine creates a wait execution (TIME or EVENT). When the duration elapses or the event is received, the engine resumes from the step’s `next`. Resume is triggered via `POST /api/v1/workflows/resume/{waitToken}` (or event publish).

### Retries

- **Where to set:** Retry is defined **per step** in the step’s **config** (e.g. in `workflow_step_definition.config_json`). Use a `retry` object with:
  - **maxAttempts** (number): How many times the step may run (including the first). Default: 1 (no retry).
  - **backoffSeconds** (number): Delay in seconds before the next attempt. Default: 0.
- **Example config:** `{ "retry": { "maxAttempts": 3, "backoffSeconds": 2 }, ...other step config... }`
- **How it works:** When a built-in (or custom) step returns `StepResult.retry()`, the engine treats it as “run this step again.” The engine:
  1. Records the attempt number in `step_execution.attempt`.
  2. If current attempt &lt; maxAttempts: waits `backoffSeconds`, then runs the same step again.
  3. If current attempt ≥ maxAttempts: marks the workflow **FAILED** with “Max retries exceeded”.
- Steps receive the current attempt (0-based) in `StepExecutionContext.getRetryCount()` so they can vary behavior per attempt.

### Durability

- Execution state (current step, context JSON) is persisted after each transition.
- On restart, in-flight executions remain in WAIT (task or wait); workers and event/scheduler continue them.

## Documented entities

- **IdempotencyKeyEntity** – scope + key → reference_id for duplicate handling.
- **WorkflowExecutionEntity** – one row per run (status, currentStep, contextJson).
- **TaskExecutionEntity** – one row per task wait (attempt, input/output JSON).
- **WaitExecutionEntity** – one row per time/event wait.

See [ARCHITECTURE.md](ARCHITECTURE.md) for domain vs persistence separation.
