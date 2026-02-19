package io.nexflow.engine.app.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nexflow.engine.app.runtime.WorkflowRuntimeService;
import io.nexflow.engine.persistence.entity.WorkflowDefinitionEntity;
import io.nexflow.engine.persistence.entity.WorkflowExecutionEntity;
import io.nexflow.engine.persistence.repository.WorkflowDefinitionRepository;
import io.nexflow.engine.persistence.repository.WorkflowExecutionRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: httpCall step with MockWebServer.
 * Validates HTTP step correctness and response in context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DisplayName("HttpCall step integration")
class HttpCallStepIntegrationTest {

    @Autowired
    private WorkflowRuntimeService runtimeService;
    @Autowired
    private WorkflowDefinitionRepository definitionRepo;
    @Autowired
    private WorkflowExecutionRepository executionRepo;
    @Autowired
    private ObjectMapper objectMapper;

    private MockWebServer mockServer;

    @BeforeEach
    void startMockServer() throws Exception {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterEach
    void stopMockServer() throws Exception {
        if (mockServer != null) {
            mockServer.shutdown();
        }
    }

    @Test
    @DisplayName("workflow with httpCall step gets response and completes")
    void workflowWithHttpCallStepGetsResponseAndCompletes() throws Exception {
        String body = "{\"ok\":true}";
        mockServer.enqueue(new MockResponse().setBody(body).setResponseCode(200));

        String baseUrl = mockServer.url("/").toString().replaceAll("/$", "");
        String workflowName = "testHttpV1";
        String v1Json = """
            {
              "version": "1",
              "name": "%s",
              "steps": [
                { "id": "s1", "name": "httpCall", "config": { "url": "%s/", "method": "GET" } }
              ]
            }
            """.formatted(workflowName, baseUrl);

        WorkflowDefinitionEntity def = new WorkflowDefinitionEntity();
        def.setName(workflowName);
        def.setVersion(1);
        def.setDescription("Test");
        def.setDefinitionJson(v1Json);
        def.setCreatedAt(Instant.now());
        definitionRepo.save(def);

        Long executionId = runtimeService.startIdempotent(
                workflowName,
                "idem-http-" + UUID.randomUUID(),
                Map.of()
        );

        WorkflowExecutionEntity exec = executionRepo.findById(executionId).orElseThrow();
        var domain = exec.toDomain();
        assertEquals("COMPLETED", domain.getStatus());
        assertTrue(domain.getContextJson().contains("ok"));
        assertTrue(domain.getContextJson().contains("true"));
    }
}
