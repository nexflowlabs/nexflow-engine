-- Nexflow Engine - Sample workflow v1: test application API
-- Uses: transform, expression, condition, script, httpCall (test service at localhost:8081).
-- Branches are stored in workflow_step_definition.branches_json (branch_key -> target step_id; null = end).
-- Run after schema.sql. Sample uses tenant_id = 'default'.
--
-- Flow: 1(transform) -> 2(expression) -> 3(condition) or 4(script) -> 5(httpCall) -> 7(done) or 8(reject)
--       Condition FALSE -> 6(transform) -> 7. Steps 7 and 8 are end steps (branch success -> null).

-- Tenant for this sample: 'default' (engine finds it without X-Tenant-Id or with X-Tenant-Id: default).

-- Delete existing sample so script is idempotent (tenant-scoped)
DELETE FROM workflow_step_definition WHERE workflow_definition_id IN (
  SELECT id FROM workflow_definition WHERE tenant_id = 'default' AND name = 'test-app-workflow'
);
DELETE FROM workflow_definition WHERE tenant_id = 'default' AND name = 'test-app-workflow';

-- Insert workflow definition (start_step_id = 1)
INSERT INTO workflow_definition (tenant_id, name, version, description, start_step_id, status, active, created_at)
VALUES (
  'default',
  'test-app-workflow',
  1,
  'Sample workflow: transform, expression, condition, script, httpCall (test app at localhost:8081)',
  1,
  'PUBLISHED',
  true,
  NOW()
);

DO $$
DECLARE
  wf_id BIGINT;
  t_id  VARCHAR(100) := 'default';
BEGIN
  SELECT id INTO wf_id FROM workflow_definition WHERE tenant_id = t_id AND name = 'test-app-workflow' ORDER BY id DESC LIMIT 1;

  -- Step 1: transform – set amount=100. Branches: success -> 2
  INSERT INTO workflow_step_definition (tenant_id, workflow_definition_id, step_id, step_name, step_type, config_json, branches_json)
  VALUES (t_id, wf_id, 1, 'init', 'transform', '{"newField":"amount","value":100}', '{"success": 2}');

  -- Step 2: expression – inputs['amount'] > 50. Branches: true -> 3, false -> 4
  INSERT INTO workflow_step_definition (tenant_id, workflow_definition_id, step_id, step_name, step_type, config_json, branches_json)
  VALUES (t_id, wf_id, 2, 'check_amount', 'expression', '{"expression":"inputs[''amount''] > 50"}', '{"true": 3, "false": 4}');

  -- Step 3: condition – amount >= 50. Branches: TRUE -> 5, FALSE -> 6
  INSERT INTO workflow_step_definition (tenant_id, workflow_definition_id, step_id, step_name, step_type, config_json, branches_json)
  VALUES (t_id, wf_id, 3, 'condition_step', 'condition', '{"field":"amount","operator":">=","value":50}', '{"TRUE": 5, "FALSE": 6}');

  -- Step 4: script – returns success with outcome "success". Branches: success -> 5
  INSERT INTO workflow_step_definition (tenant_id, workflow_definition_id, step_id, step_name, step_type, config_json, branches_json)
  VALUES (t_id, wf_id, 4, 'script_step', 'script', '{"language":"javascript","mode":"IN_PROCESS","code":"success({ outcome: ''success'' });"}', '{"success": 5}');

  -- Step 5: httpCall – GET test app approve; optional retry (maxAttempts, backoffSeconds). Branches: approve -> 7, reject -> 8, success -> 7
  INSERT INTO workflow_step_definition (tenant_id, workflow_definition_id, step_id, step_name, step_type, config_json, branches_json)
  VALUES (t_id, wf_id, 5, 'call_test_app', 'httpCall', '{"method":"GET","url":"http://localhost:8081/api/workflow-test/approve","retry":{"maxAttempts":3,"backoffSeconds":1}}', '{"approve": 7, "reject": 8, "success": 7}');

  -- Step 6: transform – merge path from condition FALSE. Branches: success -> 7
  INSERT INTO workflow_step_definition (tenant_id, workflow_definition_id, step_id, step_name, step_type, config_json, branches_json)
  VALUES (t_id, wf_id, 6, 'merge', 'transform', '{"newField":"merged","value":true}', '{"success": 7}');

  -- Step 7: end (success path). Branches: success -> null (workflow complete)
  INSERT INTO workflow_step_definition (tenant_id, workflow_definition_id, step_id, step_name, step_type, config_json, branches_json)
  VALUES (t_id, wf_id, 7, 'done', 'transform', '{"newField":"done","value":true}', '{"success": null}');

  -- Step 8: end (reject path). Branches: success -> null
  INSERT INTO workflow_step_definition (tenant_id, workflow_definition_id, step_id, step_name, step_type, config_json, branches_json)
  VALUES (t_id, wf_id, 8, 'rejected', 'transform', '{"newField":"rejected","value":true}', '{"success": null}');
END $$;

-- Verify (optional)
-- SELECT w.name, s.step_id, s.step_name, s.step_type, s.branches_json FROM workflow_definition w JOIN workflow_step_definition s ON s.workflow_definition_id = w.id WHERE w.name = 'test-app-workflow' ORDER BY s.step_id;
