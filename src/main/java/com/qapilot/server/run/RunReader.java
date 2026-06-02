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

        // tc_results 별 테이블에서 ui kind 만 추출 → {tc_id: legacy_status}
        // 파일 trace JSON 호환을 위해 status 어휘 매핑: pass→passed, fail→failed, skip→skipped.
        // 기존 ResultResponse.fromTrace 등 consumer 가 "passed"/"failed" 로 비교한다.
        Map<String, String> tcResults = new HashMap<>();
        tcResultRepository.findAllByRunId(run.getId()).stream()
                .filter(r -> "ui".equals(r.getKind()) && r.getStatus() != null)
                .forEach(r -> tcResults.put(r.getTcId(), toLegacyStatus(r.getStatus())));
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
