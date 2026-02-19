# Database Queries v1

Schema and reference SQL for Nexflow Engine persistence (PostgreSQL).

| File | Purpose |
|------|---------|
| `schema.sql` | DDL: tables and indexes matching JPA entities in `engine-persistence`. Branches live in `workflow_step_definition.branches_json` (no separate branch table). |
| `queries.sql` | Reference SELECTs aligned with repository method names (documentation / ad‑hoc use). |
| `sample_workflow_test_app.sql` | Inserts a sample workflow with `branches_json` per step: transform, expression, condition, script, httpCall (test app at localhost:8081). |

## Sample workflow (test-app-workflow)

1. Run `schema.sql`, then `sample_workflow_test_app.sql` against your PostgreSQL DB.
2. Start the **test application** (e.g. from Testing WorkSpace/test) on **port 8081** so the httpCall step can call `http://localhost:8081/api/workflow-test/approve`.
3. Start the **nexflow-engine** and run the workflow:
   - **Start:** `POST /api/v1/workflows/test-app-workflow/start` with header `Idempotency-Key: <unique-key>` and optional body `{}` or `{"amount": 100}`. Returns `executionId` and `status: STARTED` immediately; steps run asynchronously.
   - **Resume (if workflow waits):** `POST /api/v1/workflows/resume/{waitToken}` with optional body including `outcome` for branching. Returns `202 Accepted`; resume runs on a virtual thread.

The workflow runs: **transform** (set amount) → **expression** (amount > 50) → **condition** (amount >= 50) → **script** (outcome success) or merge path → **httpCall** (test app returns outcome `approve`/`reject`/`success`) → end. Use this to validate engine + test app together. See [docs/API.md](../../docs/API.md) for full API details.
