package com.qapilot.server.scenario;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.scenario.change.ChangeRequestFileStore;
import com.qapilot.server.scenario.change.domain.ChangeRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 시나리오 API 응답 enrichment 통합 테스트.
 *
 * <p>last_run_status / last_run_at / has_pending_changes 가 trace.json 및
 * change-requests 로부터 정확히 도출되어 list / get / testCases 응답에 포함되는지 확인.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
@SpringBootTest
@AutoConfigureMockMvc
class ScenarioServiceEnrichmentTest {

    private static Path targetRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChangeRequestFileStore changeRequestFileStore;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        targetRoot = Files.createTempDirectory("qapilot-scenario-enrichment-test");
        registry.add("qapilot.storage.default-target-root", () -> targetRoot.toString());
    }

    @BeforeEach
    void cleanQapilotDir() throws Exception {
        deleteRecursively(targetRoot.resolve(".qapilot"));
    }

    @Test
    void listEnrichesScenariosWithRunStatusAndPendingChanges() throws Exception {
        String accessToken = registerAdmin().get("access_token").asText();
        JsonNode service = createService(accessToken);
        String serviceId = service.get("service_id").asText();
        Path qapilotDir = Path.of(service.get("qapilot_dir").asText());

        createScenario(accessToken, serviceId, "TS-001", "로그인");
        createScenario(accessToken, serviceId, "TS-002", "회원가입");

        writeTrace(qapilotDir, "trace-1",
                "2026-05-19T00:00:00Z", "2026-05-19T00:01:00Z",
                "{\"TS-001\":\"failed\",\"TS-002\":\"passed\"}",
                "{\"TC-001\":\"failed\",\"TC-002\":\"passed\"}");

        // TS-002 에 미해결 변경 요청 추가
        changeRequestFileStore.save(qapilotDir, new ChangeRequest(
                "req-1", "TS-002", "이유", "chatbot",
                null, "2026-05-19T00:00:00Z", "2026-05-19T00:00:00Z", null, null
        ));

        mockMvc.perform(get("/api/services/" + serviceId + "/scenarios")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2))
                .andExpect(jsonPath("$.data.scenarios[?(@.ts_id == 'TS-001')].last_run_status")
                        .value("failed"))
                .andExpect(jsonPath("$.data.scenarios[?(@.ts_id == 'TS-001')].has_pending_changes")
                        .value(false))
                .andExpect(jsonPath("$.data.scenarios[?(@.ts_id == 'TS-002')].last_run_status")
                        .value("passed"))
                .andExpect(jsonPath("$.data.scenarios[?(@.ts_id == 'TS-002')].has_pending_changes")
                        .value(true));
    }

    @Test
    void getEnrichesSingleScenario() throws Exception {
        String accessToken = registerAdmin().get("access_token").asText();
        JsonNode service = createService(accessToken);
        String serviceId = service.get("service_id").asText();
        Path qapilotDir = Path.of(service.get("qapilot_dir").asText());

        createScenario(accessToken, serviceId, "TS-001", "로그인");
        writeTrace(qapilotDir, "trace-1",
                "2026-05-19T00:00:00Z", "2026-05-19T00:01:00Z",
                "{\"TS-001\":\"passed\"}",
                "{}");

        mockMvc.perform(get("/api/services/" + serviceId + "/scenarios/TS-001")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenario.ts_id").value("TS-001"))
                .andExpect(jsonPath("$.data.scenario.last_run_status").value("passed"))
                .andExpect(jsonPath("$.data.scenario.last_run_at").value("2026-05-19T00:01:00Z"))
                .andExpect(jsonPath("$.data.scenario.has_pending_changes").value(false));
    }

    @Test
    void testCasesEnrichWithRunStatus() throws Exception {
        String accessToken = registerAdmin().get("access_token").asText();
        JsonNode service = createService(accessToken);
        String serviceId = service.get("service_id").asText();
        Path qapilotDir = Path.of(service.get("qapilot_dir").asText());

        createScenario(accessToken, serviceId, "TS-001", "로그인");
        writeTrace(qapilotDir, "trace-1",
                "2026-05-19T00:00:00Z", "2026-05-19T00:01:00Z",
                "{\"TS-001\":\"failed\"}",
                "{\"TC-001\":\"failed\"}");

        mockMvc.perform(get("/api/services/" + serviceId + "/scenarios/TS-001/test-cases")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.test_cases[0].tc_id").value("TC-001"))
                .andExpect(jsonPath("$.data.test_cases[0].last_run_status").value("failed"))
                .andExpect(jsonPath("$.data.test_cases[0].last_run_at").value("2026-05-19T00:01:00Z"));
    }

    @Test
    void noTraceMeansNullStatus() throws Exception {
        String accessToken = registerAdmin().get("access_token").asText();
        JsonNode service = createService(accessToken);
        String serviceId = service.get("service_id").asText();

        createScenario(accessToken, serviceId, "TS-001", "로그인");

        mockMvc.perform(get("/api/services/" + serviceId + "/scenarios/TS-001")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scenario.last_run_status").doesNotExist())
                .andExpect(jsonPath("$.data.scenario.has_pending_changes").value(false));
    }

    private JsonNode registerAdmin() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"관리자\",\"email\":\"admin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
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
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("service");
    }

    private void createScenario(String token, String serviceId, String tsId, String name) throws Exception {
        String scenario = """
                {
                  "ts_id":"%s",
                  "name":"%s",
                  "description":"설명",
                  "trigger":"init",
                  "affected_files":["src/login.java"],
                  "domain_rules_used":[],
                  "test_cases":[{"tc_id":"TC-001","name":"정상","given":"","when":"","then":""}]
                }
                """.formatted(tsId, name);
        mockMvc.perform(post("/api/services/" + serviceId + "/scenarios")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scenario))
                .andExpect(status().isOk());
    }

    private void writeTrace(
            Path qapilotDir,
            String traceId,
            String startedAt,
            String completedAt,
            String scenarioResultsJson,
            String tcResultsJson
    ) throws Exception {
        Path tracesDir = qapilotDir.resolve("traces");
        Files.createDirectories(tracesDir);
        String trace = """
                {
                  "trace_id":"%s",
                  "command":"test",
                  "status":"completed",
                  "started_at":"%s",
                  "completed_at":"%s",
                  "scenario_results":%s,
                  "tc_results":%s
                }
                """.formatted(traceId, startedAt, completedAt, scenarioResultsJson, tcResultsJson);
        Files.writeString(tracesDir.resolve(traceId + ".json"), trace);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void deleteRecursively(Path path) throws Exception {
        if (!Files.exists(path)) return;
        try (var stream = Files.walk(path)) {
            for (Path item : stream.sorted((a, b) -> b.compareTo(a)).toList()) {
                Files.deleteIfExists(item);
            }
        }
    }
}
