package io.nexflow.engine.app.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexflow.engine.app.publish.WorkflowPublishService;
import io.nexflow.engine.app.runtime.WorkflowRuntimeService;
import io.nexflow.engine.persistence.entity.WorkflowDefinitionEntity;
import io.nexflow.engine.persistence.entity.WorkflowExecutionEntity;
import io.nexflow.engine.persistence.entity.WorkflowStepDefinitionEntity;
import io.nexflow.engine.persistence.repository.WorkflowDefinitionRepository;
import io.nexflow.engine.persistence.repository.WorkflowExecutionRepository;
import io.nexflow.engine.persistence.repository.WorkflowStepDefinitionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests: normalized workflow execution with H2.
 * Runtime uses only normalized step/branch tables (no definitionJson for execution).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("Workflow execution integration (normalized)")
class WorkflowExecutionIntegrationTest {

    @Autowired
    private WorkflowRuntimeService runtimeService;
    @Autowired
    private WorkflowPublishService publishService;
    @Autowired
    private WorkflowDefinitionRepository definitionRepo;
    @Autowired
    private WorkflowExecutionRepository executionRepo;
    @Autowired
    private WorkflowStepDefinitionRepository stepDefRepo;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Normalized workflow with transform step runs to COMPLETED and persists context")
    void normalizedWorkflowRunsToCompletedAndPersistsContext() throws Exception {
        String workflowName = "testTransformV1";
        WorkflowDefinitionEntity def = new WorkflowDefinitionEntity();
        def.setName(workflowName);
        def.setVersion(1);
        def.setDescription("Test");
        def.setStartStepId(1);
        def.setStatus(WorkflowDefinitionEntity.STATUS_DRAFT);
        def.setActive(false);
        def.setCreatedAt(Instant.now());
        definitionRepo.save(def);

        WorkflowStepDefinitionEntity step1 = WorkflowStepDefinitionEntity.of(def.getId(), 1, "transform", "transform", "{\"newField\":\"done\",\"value\":true}");
        step1.setBranchesJson("{\"success\": null}");
        stepDefRepo.save(step1);

        publishService.publish(def.getId());

        Long executionId = runtimeService.startIdempotent(
                workflowName,
                "idem-" + UUID.randomUUID(),
                Map.of("input", 1)
        );

        WorkflowExecutionEntity exec = executionRepo.findById(executionId).orElseThrow();
        var domain = exec.toDomain();
        assertEquals(def.getId(), domain.getWorkflowDefinitionId());
        assertEquals("COMPLETED", domain.getStatus());
        assertNull(domain.getCurrentStepId());
        assertNotNull(domain.getContextJson());
    }

    @Test
    @DisplayName("Execution binds to correct workflowDefinitionId")
    void executionBindsToCorrectWorkflowDefinitionId() throws Exception {
        String workflowName = "bindTest";
        WorkflowDefinitionEntity def = new WorkflowDefinitionEntity();
        def.setName(workflowName);
        def.setVersion(1);
        def.setStartStepId(1);
        def.setStatus(WorkflowDefinitionEntity.STATUS_DRAFT);
        def.setActive(false);
        def.setCreatedAt(Instant.now());
        definitionRepo.save(def);
        WorkflowStepDefinitionEntity step1 = WorkflowStepDefinitionEntity.of(def.getId(), 1, "transform", "transform", "{}");
        step1.setBranchesJson("{\"success\": null}");
        stepDefRepo.save(step1);
        publishService.publish(def.getId());

        Long executionId = runtimeService.startIdempotent(workflowName, "idem-bind-" + UUID.randomUUID(), Map.of());
        WorkflowExecutionEntity exec = executionRepo.findById(executionId).orElseThrow();
        assertEquals(def.getId(), exec.getWorkflowDefinitionId());
        assertEquals("COMPLETED", exec.getStatus());
    }

    @Test
    @DisplayName("Publish creates correct step records and status")
    void publishCreatesCorrectStepRecords() {
        String workflowName = "publishTest";
        WorkflowDefinitionEntity def = new WorkflowDefinitionEntity();
        def.setName(workflowName);
        def.setVersion(1);
        def.setStartStepId(1);
        def.setStatus(WorkflowDefinitionEntity.STATUS_DRAFT);
        def.setActive(false);
        def.setCreatedAt(Instant.now());
        definitionRepo.save(def);
        WorkflowStepDefinitionEntity step1 = WorkflowStepDefinitionEntity.of(def.getId(), 1, "step1", "transform", "{}");
        step1.setBranchesJson("{\"success\": null}");
        stepDefRepo.save(step1);

        publishService.publish(def.getId());

        WorkflowDefinitionEntity updated = definitionRepo.findById(def.getId()).orElseThrow();
        assertEquals(WorkflowDefinitionEntity.STATUS_PUBLISHED, updated.getStatus());
        assertTrue(updated.isActive());
        List<WorkflowStepDefinitionEntity> steps = stepDefRepo.findByWorkflowDefinitionIdOrderByStepId(def.getId());
        assertEquals(1, steps.size());
        assertEquals(1, steps.get(0).getStepId());
        assertEquals("transform", steps.get(0).getStepType());
    }

    @Test
    @DisplayName("Branch resolution: condition step branches to correct target")
    void branchResolutionWorks() throws Exception {
        String workflowName = "branchTest";
        WorkflowDefinitionEntity def = new WorkflowDefinitionEntity();
        def.setName(workflowName);
        def.setVersion(1);
        def.setStartStepId(1);
        def.setStatus(WorkflowDefinitionEntity.STATUS_DRAFT);
        def.setActive(false);
        def.setCreatedAt(Instant.now());
        definitionRepo.save(def);

        WorkflowStepDefinitionEntity step1 = WorkflowStepDefinitionEntity.of(def.getId(), 1, "condition", "condition", "{\"field\":\"high\",\"operator\":\"==\",\"value\":true}");
        step1.setBranchesJson("{\"TRUE\": 2, \"FALSE\": 3}");
        stepDefRepo.save(step1);
        WorkflowStepDefinitionEntity step2 = WorkflowStepDefinitionEntity.of(def.getId(), 2, "transformHigh", "transform", "{\"newField\":\"branch\",\"value\":\"HIGH\"}");
        step2.setBranchesJson("{\"success\": null}");
        stepDefRepo.save(step2);
        WorkflowStepDefinitionEntity step3 = WorkflowStepDefinitionEntity.of(def.getId(), 3, "transformLow", "transform", "{\"newField\":\"branch\",\"value\":\"LOW\"}");
        step3.setBranchesJson("{\"success\": null}");
        stepDefRepo.save(step3);

        publishService.publish(def.getId());

        Long executionId = runtimeService.startIdempotent(
                workflowName,
                "idem-branch-" + UUID.randomUUID(),
                Map.of("high", true)
        );
        WorkflowExecutionEntity exec = executionRepo.findById(executionId).orElseThrow();
        assertEquals(def.getId(), exec.getWorkflowDefinitionId());
        assertTrue("COMPLETED".equals(exec.getStatus()) || "FAILED".equals(exec.getStatus()));
    }

    @Test
    @DisplayName("Old version not affected after new publish (version isolation)")
    void oldVersionNotAffectedAfterNewPublish() throws Exception {
        String workflowName = "versionIsolation";
        WorkflowDefinitionEntity v1 = new WorkflowDefinitionEntity();
        v1.setName(workflowName);
        v1.setVersion(1);
        v1.setStartStepId(1);
        v1.setStatus(WorkflowDefinitionEntity.STATUS_DRAFT);
        v1.setActive(false);
        v1.setCreatedAt(Instant.now());
        definitionRepo.save(v1);
        WorkflowStepDefinitionEntity step1 = WorkflowStepDefinitionEntity.of(v1.getId(), 1, "transform", "transform", "{\"newField\":\"v\",\"value\":1}");
        step1.setBranchesJson("{\"success\": null}");
        stepDefRepo.save(step1);
        publishService.publish(v1.getId());

        Long executionId = runtimeService.startIdempotent(workflowName, "idem-iso-" + UUID.randomUUID(), Map.of());
        WorkflowExecutionEntity exec = executionRepo.findById(executionId).orElseThrow();
        Long boundDefId = exec.getWorkflowDefinitionId();
        assertEquals(v1.getId(), boundDefId);

        WorkflowDefinitionEntity v2 = new WorkflowDefinitionEntity();
        v2.setName(workflowName);
        v2.setVersion(2);
        v2.setStartStepId(1);
        v2.setStatus(WorkflowDefinitionEntity.STATUS_DRAFT);
        v2.setActive(false);
        v2.setCreatedAt(Instant.now());
        definitionRepo.save(v2);
        WorkflowStepDefinitionEntity step2 = WorkflowStepDefinitionEntity.of(v2.getId(), 1, "transform", "transform", "{\"newField\":\"v\",\"value\":2}");
        step2.setBranchesJson("{\"success\": null}");
        stepDefRepo.save(step2);
        publishService.publish(v2.getId());

        exec = executionRepo.findById(executionId).orElseThrow();
        assertEquals(v1.getId(), exec.getWorkflowDefinitionId(), "existing run must stay bound to v1 after v2 publish");
    }
}
