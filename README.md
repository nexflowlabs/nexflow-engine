# Nexflow Engine

Nexflow Engine is a **headless, event-driven workflow runtime** for building **durable, long-running, and idempotent workflows**. It is designed for backend engineers and platform teams who need **reliable orchestration** without UI or SaaS lock-in.

> Nexflow Engine is the **kernel** of the Nexflow platform. Studio, Cloud, and enterprise products are built **on top of this engine**, not inside it.

## Features

- Headless & embeddable
- Event-driven execution model
- Durable state (crash-safe)
- Built-in retries & failure handling
- Time & event-based waits
- Idempotency at all boundaries
- Built-in **aiDecision** step (branch on AI confidence)
- H2 dev profile; Postgres for production

## Repository layout

```
nexflow-engine/
├── engine-core/          # Pure runtime (no Spring/JPA)
├── engine-persistence/   # JPA entities & mappers to domain
├── engine-app/          # REST, scheduling, config
├── engine-api/          # API contracts
├── examples/            # basic-workflow, ai-decision-workflow
├── docker/              # docker-compose, mock-ai-server
├── docs/                # DSL-v1, Execution-Model, Built-in-Steps
├── README.md
├── LICENSE
├── CONTRIBUTING.md
└── ARCHITECTURE.md
```

## Docs

- [ARCHITECTURE.md](ARCHITECTURE.md) – Layering, domain vs persistence, built-in vs plugins
- [docs/Execution-Model.md](docs/Execution-Model.md) – Execution guarantees, idempotency, retries
- [docs/Built-in-Steps.md](docs/Built-in-Steps.md) – task, decision, wait, end, aiDecision
- [docs/DSL-v1.md](docs/DSL-v1.md) – Workflow DSL v1

## Quick start

**Prerequisites:** Java 21, Maven 3.9+. Lombok is used; if command-line build fails with annotation processor errors, build from your IDE (with Lombok plugin) or add `maven-compiler-plugin` `annotationProcessorPaths` for Lombok in the modules that use it.

**With H2 (no Postgres):**
```bash
mvn clean install
cd engine-app && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**With Docker (Postgres + Mock AI):**
```bash
cd docker && docker-compose up -d postgres mock-ai
cd ../engine-app && mvn spring-boot:run -Dspring-boot.run.profiles=prod
# Set nexflow.ai-decision.url=http://localhost:8090/confidence for aiDecision steps
```

See [docker/README.md](docker/README.md) for details.

## License

Apache License 2.0. See [LICENSE](LICENSE).
