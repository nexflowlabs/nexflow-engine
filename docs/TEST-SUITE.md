# Nexflow Engine & SDK Test Suite (OSS v1)

Layered test layout for deterministic execution and step contract validation.

## Structure

### nexflow-sdk-java
- **Contract tests**: `src/test/java/contract/`
  - `StepResultContractTest` – immutability, factory methods, branch validation
  - `StepExecutionContextContractTest` – immutability, defensive copy of inputs/stepConfig
  - `WorkflowStepContractTest` – default getType(), execute contract
  - `RetryDefinitionContractTest` – validation, getters

### nexflow-engine

- **Unit tests**: `engine-core/src/test/java/io/nexflow/engine/core/unit/`
  - `StepRegistryTest` – get by name, getAll unmodifiable
  - `RegistryDrivenRuntimeTest` – SUCCESS/BRANCH/FAILURE/RETRY outcomes, retry behavior (RETRY → same step id)
  - `ScriptStepSandboxTimeoutTest` – script timeout enforcement
  - `unit/builtin/ConditionStepTest` – operators, TRUE/FALSE branches
  - Existing: `builtin/ExpressionStepTest`, `builtin/ScriptStepTest`

- **Integration tests**: `engine-app/src/test/java/io/nexflow/engine/app/integration/`
  - `WorkflowExecutionIntegrationTest` – v1 workflow with H2, run to COMPLETED
  - `HttpCallStepIntegrationTest` – MockWebServer, httpCall step response in context
  - `AiDecisionBranchingIntegrationTest` – MockWebServer for AI endpoint, HIGH/LOW branch correctness

## Running

- **SDK**: `cd nexflow-sdk-java && mvn test`
- **Engine core (unit)**: `cd nexflow-engine && mvn test -pl engine-core`
- **Engine app (integration)**: `cd nexflow-engine && mvn test -pl engine-app` (requires SDK installed: `cd nexflow-sdk-java && mvn install -DskipTests`)

## Conventions

- JUnit 5 only; Mockito only where necessary (e.g. ScriptStep isolated client).
- Clear test names; `@DisplayName` for scenario description.
- No Spring in unit tests (plain JUnit + assertions).
- H2 for integration tests (`application-test.yml`); MockWebServer for HTTP/AI.
