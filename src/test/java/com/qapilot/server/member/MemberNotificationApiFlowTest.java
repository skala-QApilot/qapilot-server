package com.qapilot.server.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.notification.domain.Notification;
import com.qapilot.server.notification.NotificationFileStore;
import com.qapilot.server.service.store.ServiceFileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
 * 멤버/알림 API 통합 흐름 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@SpringBootTest
@AutoConfigureMockMvc
class MemberNotificationApiFlowTest {

    private static Path targetRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationFileStore notificationFileStore;

    @Autowired
    private ServiceFileStore serviceFileStore;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        targetRoot = Files.createTempDirectory("qapilot-member-notification-test");
        registry.add("qapilot.storage.default-target-root", () -> targetRoot.toString());
    }

    @BeforeEach
    void cleanQapilotDir() throws Exception {
        deleteRecursively(targetRoot.resolve(".qapilot"));
    }

    @Test
    void memberFlowWorks() throws Exception {
        String accessToken = registerAdminAndGetToken();
        String[] serviceInfo = createServiceInfo(accessToken);
        String serviceId = serviceInfo[0];
        String projectSlug = serviceInfo[1];
        String serverToken = serviceInfo[2];

        // 멤버 사전 등록 (users.json에 추가 → email 기반 중복 감지 가능)
        registerMemberViaAuth(projectSlug, serverToken, "member@example.com", "홍길동");

        // 멤버 초대
        String inviteBody = """
                {"email":"member@example.com","name":"홍길동","role":"member","team":"QA"}
                """;
        String inviteResponse = mockMvc.perform(post("/api/services/" + serviceId + "/members/invitations")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.member.email").value("member@example.com"))
                .andExpect(jsonPath("$.data.member.role").value("member"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String memberId = objectMapper.readTree(inviteResponse).get("data").get("member").get("memberId").asText();

        // 중복 초대 방지 - 동일 이메일 재초대 시 기존 멤버 반환
        mockMvc.perform(post("/api/services/" + serviceId + "/members/invitations")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.member.memberId").value(memberId));

        // 목록 조회
        mockMvc.perform(get("/api/services/" + serviceId + "/members")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1))
                .andExpect(jsonPath("$.data.members[0].role").value("member"));

        // CSV export
        mockMvc.perform(get("/api/services/" + serviceId + "/members/export")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"members.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));

        // 역할 수정
        mockMvc.perform(patch("/api/services/" + serviceId + "/members/" + memberId)
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.role").value("admin"));

        // 멤버 삭제
        mockMvc.perform(delete("/api/services/" + serviceId + "/members/" + memberId)
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNoContent());

        // 삭제 후 목록 비어있음
        mockMvc.perform(get("/api/services/" + serviceId + "/members")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    void memberNotFoundReturns404() throws Exception {
        String accessToken = registerAdminAndGetToken();
        String serviceId = createServiceAndGetId(accessToken);

        mockMvc.perform(patch("/api/services/" + serviceId + "/members/non-existent-id")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"viewer\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/services/" + serviceId + "/members/non-existent-id")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void inviteWithoutEmailReturns400() throws Exception {
        String accessToken = registerAdminAndGetToken();
        String serviceId = createServiceAndGetId(accessToken);

        mockMvc.perform(post("/api/services/" + serviceId + "/members/invitations")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"홍길동\",\"role\":\"member\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidServiceIdReturns404() throws Exception {
        String accessToken = registerAdminAndGetToken();

        mockMvc.perform(get("/api/services/non-existent-service-id/members")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void notificationFlowWorks() throws Exception {
        String accessToken = registerAdminAndGetToken();
        String serviceId = createServiceAndGetId(accessToken);

        // 테스트용 알림 데이터 준비
        String qapilotDir = serviceFileStore.findById(serviceId).get().qapilotDir();
        List<Notification> notifications = List.of(
                new Notification(UUID.randomUUID().toString(), "알림1", "내용1", "info", false, Instant.now().toString(), null),
                new Notification(UUID.randomUUID().toString(), "알림2", "내용2", "warning", false, Instant.now().toString(), null),
                new Notification(UUID.randomUUID().toString(), "알림3", "내용3", "success", true, Instant.now().toString(), Instant.now().toString())
        );
        notificationFileStore.save(Path.of(qapilotDir), notifications);
        String unreadNotificationId = notifications.get(0).notificationId();

        // 전체 목록 조회
        mockMvc.perform(get("/api/notifications?service_id=" + serviceId)
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3))
                .andExpect(jsonPath("$.data.unreadCount").value(2));

        // is_read=false 필터
        mockMvc.perform(get("/api/notifications?service_id=" + serviceId + "&is_read=false")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2));

        // 읽음 처리
        mockMvc.perform(patch("/api/notifications/" + unreadNotificationId + "?service_id=" + serviceId)
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isRead\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notification.isRead").value(true))
                .andExpect(jsonPath("$.data.notification.readAt").isNotEmpty());

        // unreadCount 감소 확인
        mockMvc.perform(get("/api/notifications?service_id=" + serviceId)
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    void notificationNotFoundReturns404() throws Exception {
        String accessToken = registerAdminAndGetToken();
        String serviceId = createServiceAndGetId(accessToken);

        mockMvc.perform(patch("/api/notifications/non-existent-id?service_id=" + serviceId)
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isRead\":true}"))
                .andExpect(status().isNotFound());
    }

    private String registerAdminAndGetToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"관리자\",\"email\":\"admin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("access_token").asText();
    }

    private String createServiceAndGetId(String accessToken) throws Exception {
        return createServiceInfo(accessToken)[0];
    }

    /** @return [serviceId, projectSlug, serverAuthToken] */
    private String[] createServiceInfo(String accessToken) throws Exception {
        String body = """
                {"name":"test service","description":"test","target_root":"%s"}
                """.formatted(targetRoot.toString());
        String response = mockMvc.perform(post("/api/services")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var service = objectMapper.readTree(response).get("data").get("service");
        return new String[]{
                service.get("service_id").asText(),
                service.get("project_slug").asText(),
                service.get("server_auth_token").asText()
        };
    }

    private void registerMemberViaAuth(String projectSlug, String serverToken, String email, String name) throws Exception {
        String body = """
                {"name":"%s","email":"%s","password":"password123","project_slug":"%s","server_auth_token":"%s"}
                """.formatted(name, email, projectSlug, serverToken);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
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
