# Nexflow Engine Architecture

## Layering

- **engine-core**: Pure runtime and domain. No Spring, no JPA. Defines workflow/step definitions, execution context, transition results, and domain models (e.g. `WorkflowExecution`).
- **engine-persistence**: JPA entities and repositories. **Maps to domain models** via mappers (e.g. `ExecutionMapper`). Application code should depend on domain types; persistence is an adapter.
- **engine-app**: REST, scheduling, configuration. Wires `WorkflowRuntime`, `AiDecisionEvaluator`, and optional plugin loading.

## Domain vs persistence

- **Domain** (engine-core): `WorkflowExecution`, `WorkflowDefinition`, `StepDefinition`, etc. Used in business logic.
- **Persistence** (engine-persistence): `WorkflowExecutionEntity`, etc. Used only inside persistence layer and mapped to/from domain at the boundary.

So: domain lives in core; persistence layer maps to domain; no business logic in entities.

## Built-in steps vs plugins

- **Built-in steps**: Implemented in engine-core (task, decision, wait, end, aiDecision). Registered explicitly via `BuiltInStepConfiguration`.
- **Plugins**: Loaded via `PluginLoader` (interface in engine-core; implementation in engine-app). Provide additional step implementations.

## AI decision

- `AiDecisionEvaluator` is an interface in engine-core. Engine-app provides an HTTP implementation when `nexflow.ai-decision.url` is set.
- Built-in step `AiDecisionStep` enforces confidence threshold and branch safety (both branches required).

## Idempotency

See [docs/Execution-Model.md](docs/Execution-Model.md) for idempotency boundaries and how execution resumes.
