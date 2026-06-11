package com.qapilot.server.run;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.run.persistence.RunEntity;
import com.qapilot.server.run.persistence.RunRepository;
import com.qapilot.server.run.persistence.TcResultRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * RunEntity (DB) → 기존 file 기반 trace JSON 의 Map 형태로 변환하는 read 어댑터.
 *
 * <p>호출자(TraceService / ResultQueryService / DashboardService 등) 가 의존하던
 * {@code Map<String, Object>} 형태를 유지하면서 데이터 출처만 file → DB 로 갈아끼운다.
 * (PR-15c — TraceFileStore 호출 사이트 → RunReader 로 전환)
 *
 * <p>출력 Map 의 키 일관성:
 * <ul>
 *   <li>top-level: trace_id, service_id, command, trigger, status, started_at, completed_at,
 *       error, confidence, total_cost, selected_total_tc_count, task_id</li>
 *   <li>options JSONB 의 키는 top-level 로 병합 (scenario_ids, staging_url, filter, tags 등)</li>
 *   <li>summary JSONB → "result_summary"</li>
 *   <li>agent_logs JSONB → "agent_logs" (List)</li>
 *   <li>tc_results 는 별 테이블 — kind='ui' 행만 tc_id → status 맵으로 변환</li>
 * </ul>
 *
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
@Component
public class RunReader {

    private final RunRepository runRepository;
    private final TcResultRepository tcResultRepository;
    private final ObjectMapper objectMapper;

    public RunReader(
            RunRepository runRepository,
            TcResultRepository tcResultRepository,
            ObjectMapper objectMapper
    ) {
        this.runRepository = runRepository;
        this.tcResultRepository = tcResultRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<Map<String, Object>> findById(String traceId) {
        try {
            return runRepository.findById(UUID.fromString(traceId)).map(this::toMap);
        } catch (IllegalArgumentException e) {
            return Optional.empty();   // trace_id 가 UUID 형식이 아님
        }
    }

    public List<Map<String, Object>> listByServiceId(UUID serviceId) {
        return runRepository.findAllByServiceIdOrderByStartedAtDesc(serviceId).stream()
                .map(this::toMap)
                .toList();
    }

    public List<Map<String, Object>> listByServiceId(String serviceIdStr) {
        return listByServiceId(UUID.fromString(serviceIdStr));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RunEntity → Map 변환
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> toMap(RunEntity run) {
        Map<String, Object> trace = new HashMap<>();
        trace.put("trace_id", run.getId().toString());
        trace.put("service_id", run.getServiceId().toString());
        trace.put("command", run.getCommand());
        trace.put("trigger", run.getTrigger());
        trace.put("status", run.getStatus());
        trace.put("started_at", toIso(run.getStartedAt()));
        trace.put("completed_at", toIso(run.getCompletedAt()));
        trace.put("error", run.getError());
        trace.put("confidence", run.getConfidence());
        trace.put("total_cost", run.getTotalCost());
        trace.put("selected_total_tc_count", run.getSelectedTotalTcCount());
        trace.put("task_id", run.getTaskId());

        // options JSONB → top-level merge
        Map<String, Object> options = decodeJsonMap(run.getOptions());
        if (options != null) {
            trace.putAll(options);
        }

        // summary JSONB → "result_summary"
        Map<String, Object> summary = decodeJsonMap(run.getSummary());
        if (summary != null) {
            trace.put("result_summary", summary);
        }

        // agent_logs JSONB → "agent_logs" (List)
        List<Object> agentLogs = decodeJsonList(run.getAgentLogs());
        if (agentLogs != null) {
            trace.put("agent_logs", agentLogs);
        }

        // tc_results — TC 의 진짜 verdict 는 cross_check kind (UI/API/DB 정합 +
        // 시나리오 의도 판정 포함). run d20fc18f 실증: ui fail 24 중 4건을
        // cross_check 가 "의도 도달" pass 로 구제 — ui kind 단독 표시는 진실 왜곡.
        // 우선순위: cross_check 있으면 그것 (pass→passed, fail→failed,
        // unverified→unverified), 없으면 ui kind fallback (skip→skipped 포함).
        Map<String, String> tcResults = new HashMap<>();
        var allResults = tcResultRepository.findAllByRunId(run.getId());
        allResults.stream()
                .filter(r -> "ui".equals(r.getKind()) && r.getStatus() != null)
                .forEach(r -> tcResults.put(r.getTcId(), toLegacyStatus(r.getStatus())));
        allResults.stream()
                .filter(r -> "cross_check".equals(r.getKind()) && r.getStatus() != null)
                .forEach(r -> {
                    String cc = toLegacyStatus(r.getStatus());
                    String ui = tcResults.get(r.getTcId());
                    // skip 보호: UI 가 검증을 안 한 TC (skipped) 를 cross_check 의
                    // 무신호 pass 가 통과로 둔갑시키면 false-positive 부활.
                    // fail/unverified 신호만 skip 을 덮을 수 있다.
                    if ("skipped".equals(ui) && "passed".equals(cc)) {
                        return;
                    }
                    tcResults.put(r.getTcId(), cc);
                });
        if (!tcResults.isEmpty()) {
            trace.put("tc_results", tcResults);
        }

        return trace;
    }

    private Map<String, Object> decodeJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private List<Object> decodeJsonList(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private String toLegacyStatus(String dbStatus) {
        return switch (dbStatus) {
            case "pass" -> "passed";
            case "fail" -> "failed";
            case "skip" -> "skipped";
            default -> dbStatus;
        };
    }
}
