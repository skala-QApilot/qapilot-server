package com.qapilot.server.artifact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 산출물 조회 API 통합 흐름 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@SpringBootTest
@AutoConfigureMockMvc
class ArtifactApiFlowTest {

    private static Path targetRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        targetRoot = Files.createTempDirectory("qapilot-artifact-api-test");
        registry.add("qapilot.storage.default-target-root", () -> targetRoot.toString());
    }

    @BeforeEach
    void cleanQapilotDir() throws Exception {
        deleteRecursively(targetRoot.resolve(".qapilot"));
    }

    @Test
    void artifactApisWork() throws Exception {
        String accessToken = registerAdmin().get("access_token").asText();
        JsonNode service = createService(accessToken);
        String serviceId = service.get("service_id").asText();
        Path qapilotDir = Path.of(service.get("qapilot_dir").asText());

        String fileId = uploadFile(accessToken, serviceId);
        mockMvc.perform(patch("/api/services/" + serviceId + "/files/" + fileId)
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"REQ-renamed.txt\",\"reflected\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.file.reflected").value(true));

        mockMvc.perform(get("/api/services/" + serviceId + "/files").header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        writeTrace(qapilotDir, "TRACE-ARTIFACT");
        mockMvc.perform(get("/api/services/" + serviceId + "/reports/TRACE-ARTIFACT")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.report.trace_id").value("TRACE-ARTIFACT"));

        mockMvc.perform(get("/api/services/" + serviceId + "/reports/TRACE-ARTIFACT/export")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trace_id").value("TRACE-ARTIFACT"));

        writeEvidence(qapilotDir, "TRACE-ARTIFACT", "TC-001");
        mockMvc.perform(get("/api/services/" + serviceId + "/evidences/TRACE-ARTIFACT")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence.trace_id").value("TRACE-ARTIFACT"));

        mockMvc.perform(get("/api/services/" + serviceId + "/evidences/TRACE-ARTIFACT/TC-001")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence.tc_id").value("TC-001"))
                .andExpect(jsonPath("$.data.evidence.file_paths.length()").value(1));

        assertThat(qapilotDir.resolve("domain/files.json")).exists();
    }

    private String uploadFile(String accessToken, String serviceId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "REQ.txt",
                "text/plain",
                "requirement".getBytes()
        );
        String response = mockMvc.perform(multipart("/api/services/" + serviceId + "/files")
                        .file(file)
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.file.version").value("v1"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("file").get("file_id").asText();
    }

    private JsonNode registerAdmin() throws Exception {
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/register")
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
        String response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/services")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("service");
    }

    private void writeTrace(Path qapilotDir, String traceId) throws Exception {
        Files.createDirectories(qapilotDir.resolve("traces"));
        String trace = """
                {
                  "trace_id":"%s",
                  "command":"test",
                  "status":"completed",
                  "completed_at":"2026-05-18T00:00:00Z",
                  "result_summary":{"ui_results_count":1}
                }
                """.formatted(traceId);
        Files.writeString(qapilotDir.resolve("traces").resolve(traceId + ".json"), trace);
    }

    private void writeEvidence(Path qapilotDir, String traceId, String tcId) throws Exception {
        Path evidenceDir = qapilotDir.resolve("evidence").resolve(traceId);
        Path tcDir = evidenceDir.resolve(tcId);
        Files.createDirectories(tcDir);
        Files.writeString(tcDir.resolve("api_log.json"), "{}");
        String index = """
                {
                  "trace_id":"%s",
                  "tc_evidences":[
                    {"tc_id":"%s","files":["api_log.json","missing.png"],"created_at":"2026-05-18T00:00:00Z"}
                  ],
                  "created_at":"2026-05-18T00:00:00Z"
                }
                """.formatted(traceId, tcId);
        Files.writeString(evidenceDir.resolve("index.json"), index);
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
