-- Nexflow Engine - Reference Queries v1
-- Named queries matching engine-persistence repository usage (documentation + ad‑hoc use).

-- =============================================================================
-- workflow_definition
-- =============================================================================

-- findByNameAndVersion
SELECT * FROM workflow_definition
WHERE name = :name AND version = :version;

-- findByNameOrderByVersionDesc
SELECT * FROM workflow_definition
WHERE name = :name
ORDER BY version DESC;

-- findByNameAndActiveTrueAndStatus (active published by name)
SELECT * FROM workflow_definition
WHERE name = :name AND active = true AND status = :status;

-- =============================================================================
-- workflow_execution
-- =============================================================================

-- findById (JPA default)
SELECT * FROM workflow_execution WHERE id = :id;

-- findByWorkflowDefinitionId (e.g. list executions for a definition)
SELECT * FROM workflow_execution
WHERE workflow_definition_id = :workflowDefinitionId
ORDER BY started_at DESC;

-- =============================================================================
-- workflow_step_definition
-- =============================================================================

-- findByWorkflowDefinitionIdOrderByStepId (includes branches_json per step)
SELECT * FROM workflow_step_definition
WHERE workflow_definition_id = :workflowDefinitionId
ORDER BY step_id;

-- =============================================================================
-- wait_execution
-- =============================================================================

-- findByStatusAndWaitTypeAndWaitUntilBefore (TIME waits due for scheduler)
SELECT * FROM wait_execution
WHERE status = :status AND wait_type = :waitType AND wait_until < :now;

-- findByStatusAndWaitTypeAndEventName (EVENT waits for event name)
SELECT * FROM wait_execution
WHERE status = :status AND wait_type = :waitType AND event_name = :eventName;

-- findByWaitTokenAndStatus (webhook resume by token)
SELECT * FROM wait_execution
WHERE wait_token = :waitToken AND status = :status;

-- =============================================================================
-- step_execution
-- =============================================================================

-- findByExecutionIdOrderByStartedAt (execution history)
SELECT * FROM step_execution
WHERE execution_id = :executionId
ORDER BY started_at;

-- =============================================================================
-- idempotency_key
-- =============================================================================

-- findByScopeAndKey (unique lookup)
SELECT * FROM idempotency_key
WHERE scope = :scope AND key_value = :keyValue;
