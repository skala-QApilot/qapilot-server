package com.qapilot.server.auth;

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
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 인증/서비스 도메인 통합 흐름 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthServiceFlowTest {

    private static Path targetRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        targetRoot = Files.createTempDirectory("qapilot-server-test");
        registry.add("qapilot.storage.default-target-root", () -> targetRoot.toString());
    }

    @BeforeEach
    void cleanQapilotDir() throws Exception {
        deleteRecursively(targetRoot.resolve(".qapilot"));
    }

    @Test
    void authAndServiceFlowWorks() throws Exception {
        JsonNode admin = registerAdmin();
        String accessToken = admin.get("access_token").asText();
        String refreshToken = admin.get("refresh_token").asText();

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@example.com"));

        JsonNode service = createService(accessToken);
        String serviceId = service.get("service_id").asText();
        String projectSlug = service.get("project_slug").asText();
        String serverToken = service.get("server_auth_token").asText();

        mockMvc.perform(get("/api/projects/" + projectSlug).header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project.project_slug").value(projectSlug))
                .andExpect(jsonPath("$.data.credentials.server_auth_token").value(serverToken));

        JsonNode member = registerMember(projectSlug, serverToken);
        assertThat(member.get("user").get("role").asText()).isEqualTo("member");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refresh_token", refreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refresh_token", refreshToken)))
                .andExpect(status().isUnauthorized());

        assertThat(targetRoot.resolve(".qapilot/users.json")).exists();
        assertThat(targetRoot.resolve(".qapilot/services.json")).exists();
        assertThat(targetRoot.resolve(".qapilot/members.json")).exists();
        assertThat(targetRoot.resolve(".qapilot/scenarios")).isDirectory();
        assertThat(serviceId).isNotBlank();
    }

    private JsonNode registerAdmin() throws Exception {
        String body = """
                {"name":"관리자","email":"admin@example.com","password":"password123"}
                """;
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("admin"))
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
                .andExpect(jsonPath("$.data.service.project_slug").value("system-under-test"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("service");
    }

    private JsonNode registerMember(String projectSlug, String serverToken) throws Exception {
        String body = """
                {
                  "name":"홍길동",
                  "email":"member@example.com",
                  "password":"password123",
                  "project_slug":"%s",
                  "server_auth_token":"%s"
                }
                """.formatted(projectSlug, serverToken);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data");
    }

    private String json(String key, String value) {
        return "{\"" + key + "\":\"" + value + "\"}";
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
