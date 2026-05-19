package com.qapilot.server.scenario;

import com.qapilot.server.trace.TraceFileStore;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 최근 trace 들로부터 시나리오 / TC 별 last_run_status 를 도출한다.
 *
 * <p>FastAPI 가 test 명령 종료 시 trace.json 에 `scenario_results`, `tc_results` 맵을
 * 보존하므로 (qapilot trace_store.update_trace), 본 aggregator 는 단일 파일 읽기로
 * Spring 응답을 enrich 한다. 디렉토리 트리 스캔 없음.
 *
 * <p>Phase D (DB) 진화 시: file 기반 trace 순회 → `scenario_run_results` 테이블 JOIN
 * 으로 자연 교체. 인터페이스 (입력 qapilotDir, 출력 Map) 는 동일.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
@Component
public class ScenarioStatusAggregator {

    private final TraceFileStore traceFileStore;

    public ScenarioStatusAggregator(TraceFileStore traceFileStore) {
        this.traceFileStore = traceFileStore;
    }

    /**
     * 시나리오 id 별 최근 실행 status (entry 없으면 한 번도 실행 안 된 시나리오).
     */
    public Map<String, RunStatus> scenarioStatuses(Path qapilotDir) {
        return aggregate(qapilotDir, "scenario_results");
    }

    /**
     * 테스트 케이스 id 별 최근 실행 status.
     */
    public Map<String, RunStatus> testCaseStatuses(Path qapilotDir) {
        return aggregate(qapilotDir, "tc_results");
    }

    private Map<String, RunStatus> aggregate(Path qapilotDir, String field) {
        // TraceFileStore.listAll 은 started_at 내림차순 정렬. 가장 최근 trace 가 우선.
        List<Map<String, Object>> traces = traceFileStore.listAll(qapilotDir);

        Map<String, RunStatus> result = new HashMap<>();
        for (Map<String, Object> trace : traces) {
            Object raw = trace.get(field);
            if (!(raw instanceof Map<?, ?> map)) {
                continue;
            }
            String when = preferredTimestamp(trace);
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String id)) {
                    continue;
                }
                if (result.containsKey(id)) {
                    // 더 최근 trace 의 status 가 이미 기록됨 — 보존
                    continue;
                }
                String status = entry.getValue() == null ? null : entry.getValue().toString();
                if (status == null || status.isBlank()) {
                    continue;
                }
                result.put(id, new RunStatus(status, when));
            }
        }
        return result;
    }

    private String preferredTimestamp(Map<String, Object> trace) {
        Object completed = trace.get("completed_at");
        if (completed != null && !completed.toString().isBlank()) {
            return completed.toString();
        }
        Object started = trace.get("started_at");
        return started == null ? null : started.toString();
    }

    public record RunStatus(String status, String at) {
    }
}
