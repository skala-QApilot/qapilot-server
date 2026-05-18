package com.qapilot.server.cli;

import static org.assertj.core.api.Assertions.assertThat;
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
 * CLI sync API 통합 흐름 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@SpringBootTest
@AutoConfigureMockMvc
class CliSyncFlowTest {

    private static Path targetRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        targetRoot = Files.createTempDirectory("qapilot-cli-sync-test");
        registry.add("qapilot.storage.default-target-root", () -> targetRoot.toString());
    }

    @BeforeEach
    void cleanQapilotDir() throws Exception {
        deleteRecursively(targetRoot.resolve(".qapilot"));
    }

    @Test
    void cliSyncStoresArtifactsByServiceToken() throws Exception {
        String accessToken = registerAdmin().get("access_token").asText();
        JsonNode service = createService(accessToken);
        String serverToken = service.get("server_auth_token").asText();
        Path qapilotDir = Path.of(service.get("qapilot_dir").asText());

        mockMvc.perform(get("/api/cli/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        mockMvc.perform(post("/api/cli/sync/scenarios")
                        .header("Authorization", bearer(serverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(syncScenariosBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.synced").value(1))
                .andExpect(jsonPath("$.data.failed").value(0));

        mockMvc.perform(post("/api/cli/sync/generated-code")
                        .header("Authorization", bearer(serverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(syncGeneratedCodeBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.synced").value(1));

        mockMvc.perform(post("/api/cli/sync/results")
                        .header("Authorization", bearer(serverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(syncResultsBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.synced").value(1));

        assertThat(qapilotDir.resolve("scenarios/TS-CLI-001.json")).exists();
        assertThat(qapilotDir.resolve("generated-code/TC-CLI-001.js")).exists();
        assertThat(qapilotDir.resolve("results/TRACE-CLI-001.json")).exists();
    }

    @Test
    void cliSyncRejectsInvalidTokenAndEmptyItems() throws Exception {
        String accessToken = registerAdmin().get("access_token").asText();
        JsonNode service = createService(accessToken);
        String serverToken = service.get("server_auth_token").asText();

        mockMvc.perform(post("/api/cli/sync/scenarios")
                        .header("Authorization", bearer("invalid"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(syncScenariosBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("CLI_SYNC_001"));

        mockMvc.perform(post("/api/cli/sync/scenarios")
                        .header("Authorization", bearer(serverToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CLI_SYNC_002"));
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

    private String syncScenariosBody() {
        return """
                {
                  "source":{"target_root":"/ignored","qapilot_dir":"/ignored/.qapilot"},
                  "items":[{"ts_id":"TS-CLI-001","name":"CLI sync scenario"}]
                }
                """;
    }

    private String syncGeneratedCodeBody() {
        return """
                {
                  "source":{"target_root":"/ignored","qapilot_dir":"/ignored/.qapilot"},
                  "items":[{"tc_id":"TC-CLI-001","path":"ignored.js","code":"console.log('ok');"}]
                }
                """;
    }

    private String syncResultsBody() {
        return """
                {
                  "source":{"target_root":"/ignored","qapilot_dir":"/ignored/.qapilot"},
                  "items":[{"trace_id":"TRACE-CLI-001","data":{"status":"completed"}}]
                }
                """;
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
