# Nexflow Engine Examples

## basic-workflow

Linear workflow: one task step then end. Use with H2 dev profile.

- **sample-workflow.json** – workflow definition
- Run engine with `spring.profiles.active=dev`, register the workflow via API, then start an execution.

## ai-decision-workflow

Workflow that uses an `aiDecision` step to branch on AI confidence.

- **sample-workflow.json** – includes `aiDecision` with `promptKey`, `confidenceThreshold`, `onHighConfidence`, `onLowConfidence`
- Requires `nexflow.ai-decision.url` (e.g. mock-ai-server in `docker/`).
- See [docker/README.md](../docker/README.md) for running with mock AI.
