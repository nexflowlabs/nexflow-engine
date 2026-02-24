# REST API

Base path: `/api/v1`. All workflow operations are under **WorkflowController** (`/api/v1/workflows`).

## Start workflow

**`POST /api/v1/workflows/{workflowName}/start`**

- **Headers:** `Idempotency-Key` (required) – duplicate requests with the same key return the existing execution ID.
- **Body:** Optional JSON object – initial workflow input (merged into execution context).
- **Response:** `200 OK` with `{ "executionId": <long>, "status": "STARTED" }`.
- **Behavior:** Idempotency is checked and the execution record is created and saved. The API returns immediately; workflow steps run asynchronously on a **virtual thread** (Java 21). No blocking until the workflow completes.

## Resume workflow (by wait token)

**`POST /api/v1/workflows/resume/{waitToken}`**

- **Path:** `waitToken` – token from `StepResult.waitForEvent(waitToken, ...)` when the step decided to wait.
- **Body:** Optional JSON – merged into execution context. For branching, include **`outcome`** (e.g. `"approve"`, `"reject"`) so the engine can resolve the next step from the workflow’s branches.
- **Response:** `202 Accepted` when a matching WAITING wait is found and resume was submitted; `404 Not Found` when no wait exists for the token; `400 Bad Request` when `waitToken` is blank.
- **Behavior:** The wait is validated in the request thread; the actual resume and subsequent steps run on a **virtual thread**. The API does not wait for the workflow to complete.

## Events (publish by name)

**`POST /api/v1/events/{eventName}`**

- Resumes all workflows waiting for the given event name (EVENT wait type). Use when steps wait on named events rather than a webhook token.

## Tenant

- **Header:** `X-Tenant-Id` (optional). When present, all data for the request is scoped to that tenant.
- **Config:** `nexflow.tenant.mode` = `single` (default) or `multi`; `nexflow.tenant.default-id` = tenant when header is absent (default: `default`).
- **Isolation:** Shared tables with `tenant_id`; Hibernate filter ensures queries only see the current tenant. Async workers set tenant from the entity before processing and clear after.

## OpenAPI / Swagger

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

## Virtual threads and async execution

- **Request handling:** With `spring.threads.virtual.enabled: true` (Java 21), each HTTP request is served on a virtual thread.
- **Start and resume:** After validating and persisting (idempotency, execution, or wait lookup), start and resume hand off execution to a virtual-thread executor and return immediately. Workflow advance and step execution run off the request thread.
