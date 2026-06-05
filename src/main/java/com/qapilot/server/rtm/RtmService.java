package com.qapilot.server.rtm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.rtm.domain.RtmRequirement;
import com.qapilot.server.rtm.domain.RtmSummary;
import com.qapilot.server.rtm.domain.RtmVersion;
import com.qapilot.server.rtm.dto.CreateRtmVersionRequest;
import com.qapilot.server.scenario.ScenarioStatusAggregator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * RTM 관리 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class RtmService {

    private final RtmReader rtmReader;
    private final ObjectMapper objectMapper;
    private final ScenarioStatusAggregator scenarioStatusAggregator;

    public RtmService(RtmReader rtmReader, ObjectMapper objectMapper,
                      ScenarioStatusAggregator scenarioStatusAggregator) {
        this.rtmReader = rtmReader;
        this.objectMapper = objectMapper;
        this.scenarioStatusAggregator = scenarioStatusAggregator;
    }

    public List<RtmVersion> list(String serviceId) {
        Map<String, ScenarioStatusAggregator.RunStatus> tcStatuses =
                scenarioStatusAggregator.testCaseStatuses(serviceId);
        // PR-15e — file → DB read. RtmReader 가 versions + requirements + tc_links JOIN 한 결과 반환.
        return rtmReader.listByServiceId(serviceId).stream()
                .map(version -> enrichVersion(version, tcStatuses))
                .toList();
    }

    /**
     * RTM 수동 생성은 PR-15h 에서 deprecated.
     * RTM 은 FastAPI 파이프라인의 _write_initial_rtm_version 이 시나리오 생성 시 자동 발급.
     * UI 가 수동 호출하는 케이스는 없어 410 Gone 으로 응답.
     */
    public RtmVersion create(String serviceId, CreateRtmVersionRequest request) {
        throw new QapilotException(ErrorCode.RTM_002,
                "RTM 수동 생성은 deprecated 됨. 시나리오 생성 시 자동 발급.");
    }

    public RtmVersion get(String serviceId, String rtmVersionId) {
        RtmVersion version = rtmReader.findById(serviceId, rtmVersionId)
                .orElseThrow(() -> new QapilotException(ErrorCode.RTM_001));
        return enrichVersion(version, scenarioStatusAggregator.testCaseStatuses(serviceId));
    }

    public List<RtmRequirement> getRequirements(String serviceId, String rtmVersionId) {
        return get(serviceId, rtmVersionId).requirements();
    }

    public RtmRequirement getRequirement(String serviceId, String rtmVersionId, String frId) {
        return getRequirements(serviceId, rtmVersionId).stream()
                .filter(req -> frId.equals(req.frId()))
                .findFirst()
                .orElseThrow(() -> new QapilotException(ErrorCode.RTM_002));
    }

    public String export(String serviceId, String rtmVersionId) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(get(serviceId, rtmVersionId));
        } catch (JsonProcessingException e) {
            throw new QapilotException(ErrorCode.FILE_001, "RTM을 직렬화할 수 없습니다.");
        }
    }

    /**
     * 저장된 RTM 버전에 동적 status/passCount/totalCount/history 채워준다.
     *
     * <p>각 FR 의 linkedTcIds 를 순회하며 ScenarioStatusAggregator 의 testCaseStatuses 결과
     * (= TC 별 최신 trace status) 로 집계. 같은 TC 가 여러 trace 에 등장해도 aggregator 가
     * 최신 1개 status 만 반환하므로 자연스럽게 "TC 단위 1회 카운트".
     *
     * <p>판정 기준:
     * <ul>
     *   <li>"충족" — 연결된 모든 TC 가 passed</li>
     *   <li>"미충족" — 하나라도 failed</li>
     *   <li>"미측정" — 연결된 TC 중 실행된 게 하나도 없음 (또는 linkedTcIds 비어있음)</li>
     * </ul>
     */
    private RtmVersion enrichVersion(RtmVersion version, Map<String, ScenarioStatusAggregator.RunStatus> tcStatuses) {
        List<RtmRequirement> enrichedReqs = new ArrayList<>();
        int satisfied = 0;
        int unsatisfied = 0;
        int unmeasured = 0;
        for (RtmRequirement req : version.requirements()) {
            List<String> linked = req.linkedTcIds() == null ? List.of() : req.linkedTcIds();
            int pass = 0;
            int fail = 0;
            int measured = 0;
            List<Map<String, Object>> history = new ArrayList<>();
            for (String tcId : linked) {
                ScenarioStatusAggregator.RunStatus runStatus = tcStatuses.get(tcId);
                if (runStatus == null) continue;
                measured++;
                if ("passed".equals(runStatus.status())) {
                    pass++;
                } else if ("failed".equals(runStatus.status())) {
                    fail++;
                }
                String tsId = tcId.contains("-TC-") ? tcId.substring(0, tcId.indexOf("-TC-")) : "";
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ts", tsId);
                row.put("tc", tcId);
                row.put("date", runStatus.at() == null ? "" : runStatus.at());
                row.put("pass", "passed".equals(runStatus.status()));
                history.add(row);
            }
            String status;
            if (measured == 0) {
                status = "미측정";
                unmeasured++;
            } else if (fail > 0) {
                status = "미충족";
                unsatisfied++;
            } else if (pass == linked.size() && pass > 0) {
                status = "충족";
                satisfied++;
            } else {
                // 일부만 측정됐고 fail 없음 — 부분 측정, 보수적으로 미측정 카운트.
                status = "미측정";
                unmeasured++;
            }
            enrichedReqs.add(new RtmRequirement(
                    req.frId(), req.content(), status, pass, linked.size(), history, linked
            ));
        }
        return new RtmVersion(
                version.rtmVersionId(),
                version.serviceId(),
                version.label(),
                version.traceId(),
                enrichedReqs,
                new com.qapilot.server.rtm.domain.RtmSummary(
                        enrichedReqs.size(), satisfied, unsatisfied, unmeasured
                ),
                version.createdAt()
        );
    }

}
