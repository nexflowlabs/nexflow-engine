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

### When execution resumes

- **After a TASK step**: Engine creates a task execution (PENDING). When a worker completes the task, the engine resumes the workflow from the step’s `onSuccess` (or `onFailure` if configured).
- **After a WAIT step**: Engine creates a wait execution (TIME or EVENT). When the duration elapses or the event is received, the engine resumes from the step’s `next`.

### Retries

- Retry behavior is defined **per step** in the workflow DSL (`retry.maxAttempts`, `retry.backoffSeconds`).
- The **engine** performs retries: it increments attempt count, applies backoff, and re-dispatches the task. The SDK only defines the retry **definition** (data); it does not execute retries.
- When attempts are exhausted, the engine follows the step’s `onFailure` or marks the workflow as FAILED.

### Durability

- Execution state (current step, context JSON) is persisted after each transition.
- On restart, in-flight executions remain in WAIT (task or wait); workers and event/scheduler continue them.

## Documented entities

- **IdempotencyKeyEntity** – scope + key → reference_id for duplicate handling.
- **WorkflowExecutionEntity** – one row per run (status, currentStep, contextJson).
- **TaskExecutionEntity** – one row per task wait (attempt, input/output JSON).
- **WaitExecutionEntity** – one row per time/event wait.

See [ARCHITECTURE.md](ARCHITECTURE.md) for domain vs persistence separation.
