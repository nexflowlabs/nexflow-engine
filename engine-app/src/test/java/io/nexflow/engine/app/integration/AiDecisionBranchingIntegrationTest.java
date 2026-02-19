package io.nexflow.engine.app.integration;

import io.nexflow.engine.app.runtime.WorkflowRuntimeService;
import io.nexflow.engine.persistence.entity.WorkflowDefinitionEntity;
import io.nexflow.engine.persistence.entity.WorkflowExecutionEntity;
import io.nexflow.engine.persistence.repository.WorkflowDefinitionRepository;
import io.nexflow.engine.persistence.repository.WorkflowExecutionRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: AI decision step branching via MockWebServer.
 * Validates HIGH/LOW branch correctness from HTTP confidence response.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("AI decision branching integration")
class AiDecisionBranchingIntegrationTest {

    private static final MockWebServer aiMockServer;

    static {
        try {
            aiMockServer = new MockWebServer();
            aiMockServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private WorkflowRuntimeService runtimeService;
    @Autowired
    private WorkflowDefinitionRepository definitionRepo;
    @Autowired
    private WorkflowExecutionRepository executionRepo;

    @AfterAll
    static void stopMockServer() throws Exception {
        aiMockServer.shutdown();
    }

    @DynamicPropertySource
    static void setAiUrl(DynamicPropertyRegistry registry) {
        registry.add("nexflow.ai-decision.url", () -> aiMockServer.url("/").toString());
    }

    @Test
    @DisplayName("AI endpoint returning high confidence leads to HIGH branch and next step")
    void highConfidenceLeadsToHighBranch() throws Exception {
        aiMockServer.enqueue(new MockResponse()
                .setBody("{\"confidence\": 0.95}")
                .addHeader("Content-Type", "application/json"));

        String workflowName = "testAiHighV1";
        String v1Json = """
            {
              "version": "1",
              "name": "%s",
              "steps": [
                { "id": "s1", "name": "aiDecision", "config": { "promptKey": "p1", "confidenceThreshold": 0.7 }, "branches": { "HIGH": "s2", "LOW": "s3" }, "next": null },
                { "id": "s2", "name": "transform", "config": { "newField": "branch", "value": "HIGH" } },
                { "id": "s3", "name": "transform", "config": { "newField": "branch", "value": "LOW" } }
              ]
            }
            """.formatted(workflowName);

        WorkflowDefinitionEntity def = new WorkflowDefinitionEntity();
        def.setName(workflowName);
        def.setVersion(1);
        def.setDescription("Test");
        def.setDefinitionJson(v1Json);
        def.setCreatedAt(Instant.now());
        definitionRepo.save(def);

        Long executionId = runtimeService.startIdempotent(
                workflowName,
                "idem-ai-" + UUID.randomUUID(),
                Map.of("input", "data")
        );

        WorkflowExecutionEntity exec = executionRepo.findById(executionId).orElseThrow();
        var domain = exec.toDomain();
        assertEquals("COMPLETED", domain.getStatus());
        assertTrue(domain.getContextJson().contains("\"branch\":\"HIGH\""));
    }

    @Test
    @DisplayName("AI endpoint returning low confidence leads to LOW branch")
    void lowConfidenceLeadsToLowBranch() throws Exception {
        aiMockServer.enqueue(new MockResponse()
                .setBody("{\"confidence\": 0.2}")
                .addHeader("Content-Type", "application/json"));

        String workflowName = "testAiLowV1";
        String v1Json = """
            {
              "version": "1",
              "name": "%s",
              "steps": [
                { "id": "s1", "name": "aiDecision", "config": { "promptKey": "p1", "confidenceThreshold": 0.7 }, "branches": { "HIGH": "s2", "LOW": "s3" } },
                { "id": "s2", "name": "transform", "config": { "newField": "branch", "value": "HIGH" } },
                { "id": "s3", "name": "transform", "config": { "newField": "branch", "value": "LOW" } }
              ]
            }
            """.formatted(workflowName);

        WorkflowDefinitionEntity def = new WorkflowDefinitionEntity();
        def.setName(workflowName);
        def.setVersion(1);
        def.setDescription("Test");
        def.setDefinitionJson(v1Json);
        def.setCreatedAt(Instant.now());
        definitionRepo.save(def);

        Long executionId = runtimeService.startIdempotent(
                workflowName,
                "idem-ai-low-" + UUID.randomUUID(),
                Map.of()
        );

        WorkflowExecutionEntity exec = executionRepo.findById(executionId).orElseThrow();
        var domain = exec.toDomain();
        assertEquals("COMPLETED", domain.getStatus());
        assertTrue(domain.getContextJson().contains("\"branch\":\"LOW\""));
    }
}
