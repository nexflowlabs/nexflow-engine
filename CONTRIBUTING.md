# Contributing to Nexflow Engine

Thank you for considering contributing.

## Development setup

1. Clone the repository.
2. Build: `mvn clean install`
3. Run with H2 (no Postgres): `cd engine-app && mvn spring-boot:run -Dspring-boot.run.profiles=dev`
4. Run tests: `mvn test`

## Code style

- Java 21. Follow standard Java style (e.g. Google Java Style or project formatter).
- Keep engine-core free of Spring and JPA; use domain models and interfaces only.
- Persistence layer: map entities to/from domain in mappers; do not expose entities to app logic.

## Pull requests

- Open an issue or PR with a clear description.
- Ensure tests pass and new code is covered where appropriate.
- Docs under `docs/` should be updated if behavior or contracts change.

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
