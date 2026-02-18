# Workflow DSL v1

Formal spec for workflow definitions. JSON schema: see [nexflow-sdk-java/docs/workflow-dsl-v1-schema.json](https://github.com/nexflow/nexflow-sdk-java/blob/main/docs/workflow-dsl-v1-schema.json) (in SDK repo).

## Root

- **name** (string, required)
- **version** (integer, required)
- **start** (string, required) – first step name
- **steps** (object, required) – step name → step definition
- **description** (string, optional)

## Step types

- `task` – worker task; `task`, `retry`, `onSuccess`, `onFailure`
- `decision` – branch; `expression`, `onTrue`, `onFalse`
- `wait` – pause; `duration` (ISO-8601) or `event`, and `next`
- `end` – terminate; optional `status`
- `aiDecision` – AI branch; `promptKey`, `confidenceThreshold`, `onHighConfidence`, `onLowConfidence`

## Version

DSL version is 1. Engine and SDK use this shape for parsing and validation.
