package com.qapilot.server.rtm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.scenario.change.ChangeRequestFileStore;
import com.qapilot.server.scenario.change.domain.ChangeRequest;
import com.qapilot.server.service.store.ServiceFileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
 * RTM/시나리오 관리/재테스트/그래프 API 통합 흐름 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@SpringBootTest
@AutoConfigureMockMvc
class RtmScenarioManagementApiFlowTest {

    private static Path targetRoot;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServiceFileStore serviceFileStore;

    @Autowired
    private ChangeRequestFileStore changeRequestFileStore;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        targetRoot = Files.createTempDirectory("qapilot-rtm-test");
        registry.add("qapilot.storage.default-target-root", () -> targetRoot.toString());
    }

    @BeforeEach
    void cleanQapilotDir() throws Exception {
        deleteRecursively(targetRoot.resolve(".qapilot"));
    }

    // ──────────────────── RTM ────────────────────

    @Test
    void rtmFlowWorks() throws Exception {
        String token = registerAdminAndGetToken();
        String serviceId = createServiceAndGetId(token);

        // 버전 생성
        String body = """
                {
                  "label": "v1.0",
                  "traceId": null,
                  "requirements": [
                    {"frId":"REQ-001","content":"로그인","status":"충족","passCount":3,"totalCount":3,"history":[]},
                    {"frId":"REQ-002","content":"회원가입","status":"미충족","passCount":0,"totalCount":2,"history":[]},
                    {"frId":"REQ-003","content":"결제","status":"미측정","passCount":0,"totalCount":0,"history":[]}
                  ]
                }
                """;
        String createResp = mockMvc.perform(post("/api/services/" + serviceId + "/rtm-versions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version.label").value("v1.0"))
                .andExpect(jsonPath("$.data.version.summary.total").value(3))
                .andExpect(jsonPath("$.data.version.summary.satisfied").value(1))
                .andExpect(jsonPath("$.data.version.summary.unsatisfied").value(1))
                .andExpect(jsonPath("$.data.version.summary.unmeasured").value(1))
                .andReturn().getResponse().getContentAsString();
        String versionId = objectMapper.readTree(createResp).get("data").get("version").get("rtmVersionId").asText();

        // 버전 목록 조회
        mockMvc.perform(get("/api/services/" + serviceId + "/rtm-versions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        // 단건 조회
        mockMvc.perform(get("/api/services/" + serviceId + "/rtm-versions/" + versionId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version.rtmVersionId").value(versionId));

        // requirements 조회
        mockMvc.perform(get("/api/services/" + serviceId + "/rtm-versions/" + versionId + "/requirements")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3));

        // requirement 단건 조회
        mockMvc.perform(get("/api/services/" + serviceId + "/rtm-versions/" + versionId + "/requirements/REQ-001")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requirement.frId").value("REQ-001"))
                .andExpect(jsonPath("$.data.requirement.status").value("충족"));

        // 없는 requirement → 404
        mockMvc.perform(get("/api/services/" + serviceId + "/rtm-versions/" + versionId + "/requirements/REQ-999")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());

        // export
        mockMvc.perform(get("/api/services/" + serviceId + "/rtm-versions/" + versionId + "/export")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"rtm_" + versionId + ".json\""));
    }

    // ──────────────────── 시나리오 버전 ────────────────────

    @Test
    void scenarioVersionFlowWorks() throws Exception {
        String token = registerAdminAndGetToken();
        String serviceId = createServiceAndGetId(token);

        // 시나리오 먼저 생성
        createScenario(token, serviceId, "TS-001", "로그인");

        // 버전 1 생성
        String v1Resp = mockMvc.perform(post("/api/services/" + serviceId + "/scenario-versions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"v1.0\",\"description\":\"초기 버전\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.version.label").value("v1.0"))
                .andExpect(jsonPath("$.data.version.isFavorite").value(false))
                .andReturn().getResponse().getContentAsString();
        String v1Id = objectMapper.readTree(v1Resp).get("data").get("version").get("versionId").asText();

        // 시나리오 추가 후 버전 2 생성
        createScenario(token, serviceId, "TS-002", "회원가입");
        String v2Resp = mockMvc.perform(post("/api/services/" + serviceId + "/scenario-versions")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"v2.0\",\"description\":\"두 번째 버전\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String v2Id = objectMapper.readTree(v2Resp).get("data").get("version").get("versionId").asText();

        // 목록 조회 (createdAt desc)
        mockMvc.perform(get("/api/services/" + serviceId + "/scenario-versions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2));

        // diff 조회 — v2에서 TS-002 added
        mockMvc.perform(get("/api/services/" + serviceId + "/scenario-versions/" + v2Id + "/diff")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diff.added").isArray())
                .andExpect(jsonPath("$.data.diff.removed").isArray());

        // diff for v1 — 직전 버전 없음, added=전체
        mockMvc.perform(get("/api/services/" + serviceId + "/scenario-versions/" + v1Id + "/diff")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diff.added[0]").value("TS-001"))
                .andExpect(jsonPath("$.data.diff.removed").isEmpty());

        // isFavorite 수정
        mockMvc.perform(patch("/api/services/" + serviceId + "/scenario-versions/" + v1Id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isFavorite\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version.isFavorite").value(true));

        // 삭제
        mockMvc.perform(delete("/api/services/" + serviceId + "/scenario-versions/" + v1Id)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        // 삭제 후 없는 버전 → 404
        mockMvc.perform(patch("/api/services/" + serviceId + "/scenario-versions/" + v1Id)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isFavorite\":false}"))
                .andExpect(status().isNotFound());
    }

    // ──────────────────── 시나리오 그룹 ────────────────────

    @Test
    void scenarioGroupFlowWorks() throws Exception {
        String token = registerAdminAndGetToken();
        String serviceId = createServiceAndGetId(token);

        // 그룹 생성
        String createResp = mockMvc.perform(post("/api/services/" + serviceId + "/scenario-groups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"스모크 테스트\",\"scenarioIds\":[\"TS-001\"],\"tcIds\":[\"TC-001\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.group.name").value("스모크 테스트"))
                .andReturn().getResponse().getContentAsString();
        String groupId = objectMapper.readTree(createResp).get("data").get("group").get("groupId").asText();

        // 목록 조회
        mockMvc.perform(get("/api/services/" + serviceId + "/scenario-groups")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        // 스케줄 추가
        mockMvc.perform(post("/api/services/" + serviceId + "/scenario-groups/" + groupId + "/schedule")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cron\":\"0 9 * * 1\",\"timezone\":\"Asia/Seoul\",\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.group.schedule.cron").value("0 9 * * 1"))
                .andExpect(jsonPath("$.data.group.schedule.timezone").value("Asia/Seoul"));

        // 이름 수정
        mockMvc.perform(patch("/api/services/" + serviceId + "/scenario-groups/" + groupId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"회귀 테스트\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.group.name").value("회귀 테스트"));

        // 삭제
        mockMvc.perform(delete("/api/services/" + serviceId + "/scenario-groups/" + groupId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/services/" + serviceId + "/scenario-groups")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));
    }

    // ──────────────────── 변경 요청 ────────────────────

    @Test
    void changeRequestFlowWorks() throws Exception {
        String token = registerAdminAndGetToken();
        String serviceId = createServiceAndGetId(token);
        String qapilotDir = serviceFileStore.findById(serviceId).get().qapilotDir();

        // 테스트용 변경 요청 파일 직접 생성
        String now = Instant.now().toString();
        ChangeRequest cr = new ChangeRequest(
                UUID.randomUUID().toString(), "TS-001", "시나리오 갱신 필요", "file",
                "pending", now, now, null, null
        );
        changeRequestFileStore.save(Path.of(qapilotDir), cr);
        String requestId = cr.requestId();

        // 목록 조회
        mockMvc.perform(get("/api/services/" + serviceId + "/scenario-change-requests")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        // status 필터
        mockMvc.perform(get("/api/services/" + serviceId + "/scenario-change-requests?status=pending")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        // status 변경 (approved)
        mockMvc.perform(patch("/api/services/" + serviceId + "/scenario-change-requests/" + requestId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"approved\",\"reviewer\":\"reviewer@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.request.status").value("approved"))
                .andExpect(jsonPath("$.data.request.reviewer").value("reviewer@example.com"))
                .andExpect(jsonPath("$.data.request.reviewedAt").isNotEmpty());

        // 잘못된 status → 400
        mockMvc.perform(patch("/api/services/" + serviceId + "/scenario-change-requests/" + requestId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"invalid-status\"}"))
                .andExpect(status().isBadRequest());
    }

    // ──────────────────── 재테스트 ────────────────────

    @Test
    void retestFlowWorks() throws Exception {
        String token = registerAdminAndGetToken();
        String serviceId = createServiceAndGetId(token);

        // 재테스트 그룹 생성
        mockMvc.perform(post("/api/services/" + serviceId + "/retest-groups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceTraceId\":\"TRACE-001\",\"failedTcIds\":[\"TC-001\",\"TC-002\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.retestGroup.status").value("pending"))
                .andExpect(jsonPath("$.data.retestGroup.sourceTraceId").value("TRACE-001"));

        // 목록 조회
        mockMvc.perform(get("/api/services/" + serviceId + "/retest-groups")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(1));

        // 빈 failedTcIds → 400
        mockMvc.perform(post("/api/services/" + serviceId + "/retest-groups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceTraceId\":\"TRACE-001\",\"failedTcIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    // ──────────────────── 그래프 ────────────────────

    @Test
    void graphFlowWorks() throws Exception {
        String token = registerAdminAndGetToken();
        String serviceId = createServiceAndGetId(token);

        createScenario(token, serviceId, "TS-001", "로그인");
        createScenario(token, serviceId, "TS-002", "회원가입");

        // scenario-graph
        mockMvc.perform(get("/api/services/" + serviceId + "/scenario-graph")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.graph.nodes").isArray())
                .andExpect(jsonPath("$.data.graph.edges").isArray())
                .andExpect(jsonPath("$.data.graph.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.graph.nodes.length()").value(2));

        // scenario-flow
        mockMvc.perform(get("/api/services/" + serviceId + "/scenario-flow")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flow.flows").isArray())
                .andExpect(jsonPath("$.data.flow.flows.length()").value(2))
                .andExpect(jsonPath("$.data.flow.generatedAt").isNotEmpty());
    }

    // ──────────────────── 헬퍼 ────────────────────

    private String registerAdminAndGetToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"관리자\",\"email\":\"admin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("access_token").asText();
    }

    private String createServiceAndGetId(String token) throws Exception {
        String body = """
                {"name":"test service","description":"test","target_root":"%s"}
                """.formatted(targetRoot.toString());
        String response = mockMvc.perform(post("/api/services")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("service").get("service_id").asText();
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
