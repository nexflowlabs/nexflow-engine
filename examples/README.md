# Nexflow Engine Examples

Workflows are defined in the **database** (PostgreSQL). Use the schema and sample SQL in [db/queries/v1/](../db/queries/v1/) to create workflows. The engine loads definitions via **WorkflowProvider** (cached in memory); it does not load from JSON files. The JSON files here are **reference shapes** for the registry-driven DSL and for mapping to DB columns.

## API (current)

- **Start workflow:** `POST /api/v1/workflows/{workflowName}/start`  
  Header: `Idempotency-Key: <unique-key>`. Optional body = initial input.  
  Returns `{ "executionId", "status": "STARTED" }` immediately; steps run on a virtual thread.
- **Resume workflow:** `POST /api/v1/workflows/resume/{waitToken}`  
  Optional body with `outcome` for branching. Returns `202 Accepted`; resume runs asynchronously.

See [docs/API.md](../docs/API.md) for full details.

## basic-workflow (v1 registry-driven)

- **sample-workflow-v1.json** – Reference shape: steps array with numeric `step_id`, `step_name`, `step_type`, `config`, `branches` (branch key → step id). Step **name** in the engine is the registry key (`step_type` in DB). No separate webhook path; resume is under `/api/v1/workflows/resume/{waitToken}`.
- Workflows are stored in `workflow_definition` and `workflow_step_definition` (`config_json`, `branches_json`). Run [db/queries/v1/schema.sql](../db/queries/v1/schema.sql) and insert your workflow, then start with the API above.

## ai-decision-workflow

- **sample-workflow.json** – Legacy type-based DSL (AI_DECISION, TASK, END) for reference only.
- For the current engine use the **registry-driven** shape: steps in DB with `step_type: "aiDecision"`, `config_json` with `promptKey`, `confidenceThreshold`, and `branches_json` mapping `HIGH`/`LOW` to step ids. Requires `nexflow.ai-decision.url` (e.g. [docker/](../docker/) mock-ai-server).
- See [docs/DSL-v1-Registry-Driven.md](../docs/DSL-v1-Registry-Driven.md) and [docs/Built-in-Steps.md](../docs/Built-in-Steps.md).

## Retry

Per-step retry is configured in the step’s **config** (e.g. in `config_json`):

```json
{
  "retry": { "maxAttempts": 3, "backoffSeconds": 2 },
  "method": "GET",
  "url": "https://api.example.com/call"
}
```

See [docs/Execution-Model.md](../docs/Execution-Model.md) and [docs/Built-in-Steps.md](../docs/Built-in-Steps.md#retry-all-built-in-steps).

## Sample workflow (test-app-workflow)

A full runnable example is in [db/queries/v1/sample_workflow_test_app.sql](../db/queries/v1/sample_workflow_test_app.sql): transform, expression, condition, script, httpCall (with optional retry), and branches. See [db/queries/v1/README.md](../db/queries/v1/README.md) for how to run it and call the start/resume APIs.
