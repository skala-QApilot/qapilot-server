package com.qapilot.server.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.fastapi.FastApiAgentClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 핵심 UI API 통합 흐름 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@SpringBootTest
@AutoConfigureMockMvc
class CoreUiApiFlowTest {

    private static Path targetRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FastApiAgentClient fastApiAgentClient;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        targetRoot = Files.createTempDirectory("qapilot-core-api-test");
        registry.add("qapilot.storage.default-target-root", () -> targetRoot.toString());
    }

    @BeforeEach
    void cleanQapilotDir() throws Exception {
        deleteRecursively(targetRoot.resolve(".qapilot"));
    }

    @Test
    void coreUiApiFlowWorks() throws Exception {
        String accessToken = registerAdmin().get("access_token").asText();
        JsonNode service = createService(accessToken);
        String serviceId = service.get("service_id").asText();
        Path qapilotDir = Path.of(service.get("qapilot_dir").asText());

        createScenario(accessToken, serviceId);
        mockMvc.perform(get("/api/services/" + serviceId + "/scenarios").header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.scenarios[0].ts_id").value("TS-001"));

        writeTrace(qapilotDir, "TRACE-001", "completed");
        writeTrace(qapilotDir, "TRACE-002", "running");
        when(fastApiAgentClient.startTestRun(serviceId, qapilotDir.toString(), List.of("TS-001"), "all", null))
                .thenReturn("TRACE-002");

        mockMvc.perform(post("/api/services/" + serviceId + "/runs")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenario_ids\":[\"TS-001\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.run_id").value("TRACE-002"));

        mockMvc.perform(get("/api/services/" + serviceId + "/runs/active").header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        mockMvc.perform(get("/api/services/" + serviceId + "/results").header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));

        mockMvc.perform(get("/api/dashboard/summary?service_id=" + serviceId).header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenarios_count").value(1))
                .andExpect(jsonPath("$.data.recent_runs.length()").value(2));

        assertThat(qapilotDir.resolve("scenarios/TS-001.json")).exists();
        assertThat(qapilotDir.resolve("traces/TRACE-001.json")).exists();
    }

    private JsonNode registerAdmin() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"관리자\",\"email\":\"admin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data");
    }

    private JsonNode createService(String accessToken) throws Exception {
        String body = """
                {"name":"system under test","description":"SUT","target_root":"%s"}
                """.formatted(targetRoot.toString());
        String response = mockMvc.perform(post("/api/services")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("service");
    }

    private void createScenario(String accessToken, String serviceId) throws Exception {
        String scenario = """
                {
                  "ts_id":"TS-001",
                  "name":"로그인",
                  "description":"사용자 로그인",
                  "trigger":"init",
                  "affected_files":[],
                  "domain_rules_used":[],
                  "test_cases":[{"tc_id":"TC-001","name":"정상 로그인"}]
                }
                """;
        mockMvc.perform(post("/api/services/" + serviceId + "/scenarios")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scenario))
                .andExpect(status().isOk());
    }

    private void writeTrace(Path qapilotDir, String traceId, String status) throws Exception {
        Files.createDirectories(qapilotDir.resolve("traces"));
        String trace = """
                {
                  "trace_id":"%s",
                  "command":"test",
                  "status":"%s",
                  "started_at":"2026-05-18T00:00:00Z",
                  "completed_at":null,
                  "error":null,
                  "confidence":null,
                  "total_cost":0.0,
                  "result_summary":{"ui_results_count":0}
                }
                """.formatted(traceId, status);
        Files.writeString(qapilotDir.resolve("traces").resolve(traceId + ".json"), trace);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void deleteRecursively(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            for (Path item : stream.sorted((a, b) -> b.compareTo(a)).toList()) {
                Files.deleteIfExists(item);
            }
        }
    }
}
