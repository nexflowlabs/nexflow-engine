-- Nexflow Engine - Database Schema v1
-- PostgreSQL (uses jsonb). Aligns with engine-persistence JPA entities.

-- Workflow definitions (name + version unique).
CREATE TABLE IF NOT EXISTS workflow_definition (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    version             INT NOT NULL,
    description         VARCHAR(1024),
    start_step_id       INT,
    status              VARCHAR(64) NOT NULL,
    active              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ,
    UNIQUE (name, version)
);

-- Workflow runs (one per started workflow).
CREATE TABLE IF NOT EXISTS workflow_execution (
    id                      BIGSERIAL PRIMARY KEY,
    workflow_definition_id  BIGINT NOT NULL REFERENCES workflow_definition(id),
    status                  VARCHAR(64),
    current_step_id         INT,
    context_json            JSONB,
    started_at              TIMESTAMPTZ,
    updated_at              TIMESTAMPTZ
);

-- Normalized step definitions per workflow (one row per step).
-- branches_json: branch key -> target step_id (e.g. {"success": 2, "true": 3}). Null value = workflow complete.
CREATE TABLE IF NOT EXISTS workflow_step_definition (
    id                      BIGSERIAL PRIMARY KEY,
    workflow_definition_id  BIGINT NOT NULL REFERENCES workflow_definition(id),
    step_id                 INT NOT NULL,
    step_name               VARCHAR(255),
    step_type               VARCHAR(128) NOT NULL,
    config_json             JSONB,
    branches_json           JSONB,
    UNIQUE (workflow_definition_id, step_id)
);

-- Waits (TIME or EVENT) for scheduler and webhook resume.
CREATE TABLE IF NOT EXISTS wait_execution (
    id                      BIGSERIAL PRIMARY KEY,
    execution_id            BIGINT NOT NULL,
    step_id                 INT NOT NULL,
    wait_type               VARCHAR(32) NOT NULL,
    wait_until              TIMESTAMPTZ,
    event_name              VARCHAR(255),
    wait_token              VARCHAR(512),
    next_step_id_when_resumed INT,
    status                  VARCHAR(32) NOT NULL,
    created_at              TIMESTAMPTZ,
    updated_at              TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_wait_execution_status_type_until
    ON wait_execution (status, wait_type, wait_until);
CREATE INDEX IF NOT EXISTS idx_wait_execution_status_type_event
    ON wait_execution (status, wait_type, event_name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_wait_execution_token_status
    ON wait_execution (wait_token, status) WHERE wait_token IS NOT NULL;

-- Step execution history (one row per step run).
CREATE TABLE IF NOT EXISTS step_execution (
    id          BIGSERIAL PRIMARY KEY,
    execution_id BIGINT NOT NULL,
    step_id     INT NOT NULL,
    status      VARCHAR(64),
    attempt     INT NOT NULL,
    input_json  JSONB,
    output_json JSONB,
    started_at  TIMESTAMPTZ,
    ended_at    TIMESTAMPTZ
);

-- Idempotency keys (scope + key_value unique).
CREATE TABLE IF NOT EXISTS idempotency_key (
    id          BIGSERIAL PRIMARY KEY,
    scope       VARCHAR(255) NOT NULL,
    key_value   VARCHAR(512) NOT NULL,
    reference_id BIGINT,
    created_at  TIMESTAMPTZ,
    UNIQUE (scope, key_value)
);
