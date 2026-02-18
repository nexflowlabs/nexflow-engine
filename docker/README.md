# Docker quickstart

## Run Postgres + Mock AI server

```bash
# From engine repo root
cd docker
docker-compose up -d postgres mock-ai
```

- **Postgres**: `localhost:5432` (nexflow/nexflow/nexflow)
- **Mock AI**: `http://localhost:8090/confidence` (POST with `{"promptKey":"...", "context":{}}` → `{"confidence":0.9}`)

## Run engine locally against Docker services

1. Start Postgres and mock-ai (above).
2. Set `nexflow.ai-decision.url=http://localhost:8090/confidence` (e.g. in `application.yml` or env).
3. Run engine with prod profile (Postgres):
   ```bash
   cd engine-app && mvn spring-boot:run -Dspring-boot.run.profiles=prod
   ```
4. Or use H2 + mock AI (no Postgres):
   - Profile `dev` (H2) and set `nexflow.ai-decision.url=http://localhost:8090/confidence`.

## Build mock-ai-server (standalone)

```bash
cd docker/mock-ai-server
mvn package
java -jar target/mock-ai-server-1.0.0-SNAPSHOT.jar
```

Then point engine at `http://localhost:8090/confidence`.
