# Workflow DSL v1

Formal spec for workflow definitions. The **engine** uses the **registry-driven** model and loads workflows from the **database**; see [DSL-v1-Registry-Driven.md](DSL-v1-Registry-Driven.md) for how definitions are stored and executed.

Optional JSON schema (e.g. for validation in tooling): see [nexflow-sdk-java/docs/workflow-dsl-v1-schema.json](https://github.com/nexflow/nexflow-sdk-java/blob/main/docs/workflow-dsl-v1-schema.json) (in SDK repo) if available.

## Root (legacy / alternate object-based DSL)

This shape is a **legacy/alternate** representation; the engine does not load from this JSON at runtime. It is useful for documentation and tooling.

- **name** (string, required)
- **version** (integer or string, required)
- **start** (string, required) – first step name (or id)
- **steps** (object, required) – step name → step definition
- **description** (string, optional)

## Step types (legacy naming)

- **task** – worker task; `task`, `retry`, `onSuccess`, `onFailure`
- **decision** – branch; `expression`, `onTrue`, `onFalse`
- **wait** – pause; `duration` (ISO-8601) or `event`, and `next`
- **end** – terminate; optional `status`
- **aiDecision** – AI branch; `promptKey`, `confidenceThreshold`, `onHighConfidence`, `onLowConfidence`
- **script** – in-process script (e.g. JavaScript via Rhino); `code`, optional `timeoutMs`; call `success()`, `failure()`, or `branch()`

## Engine model (registry-driven + DB)

The engine stores workflows in PostgreSQL:

- **workflow_definition**: `name`, `version`, `description`, `start_step_id`, `status`, `active`
- **workflow_step_definition**: `step_id`, `step_name`, `step_type`, `config_json`, `branches_json`

Steps are executed by **step_type** (registry key) via `StepRegistry`; no switch on type at runtime. Retry is per step via **config**: `retry.maxAttempts`, `retry.backoffSeconds`. Workflows are loaded by **WorkflowProvider** (see [DSL-v1-Registry-Driven.md](DSL-v1-Registry-Driven.md)).

## Version

DSL version is 1. Engine and SDK use the registry-driven shape and DB schema for parsing, storage, and execution.
