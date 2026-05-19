package com.qapilot.server.trace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * trace 조회 API 흐름 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
@SpringBootTest
@AutoConfigureMockMvc
class TraceApiFlowTest {

    private static Path targetRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        targetRoot = Files.createTempDirectory("qapilot-trace-api-test");
        registry.add("qapilot.storage.default-target-root", () -> targetRoot.toString());
    }

    @BeforeEach
    void cleanQapilotDir() throws Exception {
        deleteRecursively(targetRoot.resolve(".qapilot"));
    }

    @Test
    void traceLookupReturnsStoredTrace() throws Exception {
        String accessToken = registerAdmin().get("access_token").asText();
        JsonNode service = createService(accessToken);
        String serviceId = service.get("service_id").asText();
        Path qapilotDir = Path.of(service.get("qapilot_dir").asText());

        writeTrace(qapilotDir, "TRACE-RUN-001", "running");

        mockMvc.perform(get("/api/services/" + serviceId + "/traces/TRACE-RUN-001")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trace.trace_id").value("TRACE-RUN-001"))
                .andExpect(jsonPath("$.data.trace.status").value("running"))
                .andExpect(jsonPath("$.data.trace.command").value("generate_scenarios"));
    }

    @Test
    void traceLookupReturnsNotFoundWhenMissing() throws Exception {
        String accessToken = registerAdmin().get("access_token").asText();
        String serviceId = createService(accessToken).get("service_id").asText();

        mockMvc.perform(get("/api/services/" + serviceId + "/traces/UNKNOWN-TRACE")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRACE_001"));
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

    private void writeTrace(Path qapilotDir, String traceId, String status) throws Exception {
        Files.createDirectories(qapilotDir.resolve("traces"));
        String trace = """
                {
                  "trace_id":"%s",
                  "command":"generate_scenarios",
                  "status":"%s",
                  "started_at":"2026-05-19T00:00:00Z"
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
