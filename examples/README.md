# Nexflow Engine Examples

## basic-workflow (v1 registry-driven)

- **sample-workflow-v1.json** – v1 DSL: steps array with `id`, `name`, `config`, `next`. No `type: TASK` or `END`; step **name** (e.g. `helloTask`) is resolved from StepRegistry.
- **sample-workflow.json** – legacy type-based DSL (optional).
- Run engine with `spring.profiles.active=dev`, register the workflow (v1 JSON), then start an execution. The engine detects v1 when `steps` is a JSON array.

## ai-decision-workflow

Workflow that uses an `aiDecision` step to branch on AI confidence.

- **sample-workflow.json** – type-based DSL with `aiDecision`.
- For v1 registry-driven, use steps array with `name: "aiDecision"`, `config: { endpoint, confidenceThreshold }`, `branches: { HIGH: "...", LOW: "..." }`.
- Requires `nexflow.ai-decision.url` (e.g. mock-ai-server in `docker/`).
- See [docker/README.md](../docker/README.md) and [docs/DSL-v1-Registry-Driven.md](../docs/DSL-v1-Registry-Driven.md).
