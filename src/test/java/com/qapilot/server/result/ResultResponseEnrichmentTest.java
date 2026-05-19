package com.qapilot.server.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.qapilot.server.result.dto.ResultResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * ResultResponse 의 passCount / failCount / totalTcCount 도출 검증.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
class ResultResponseEnrichmentTest {

    @Test
    void countsPassedAndFailedFromTcResults() {
        Map<String, Object> trace = baseTrace();
        Map<String, String> tcResults = new LinkedHashMap<>();
        tcResults.put("TC-001", "passed");
        tcResults.put("TC-002", "passed");
        tcResults.put("TC-003", "failed");
        tcResults.put("TC-004", "skipped"); // 알 수 없는 값 → 무시
        trace.put("tc_results", tcResults);

        ResultResponse res = ResultResponse.fromTrace(trace);

        assertThat(res.passCount()).isEqualTo(2);
        assertThat(res.failCount()).isEqualTo(1);
        assertThat(res.totalTcCount()).isEqualTo(4);
    }

    @Test
    void zeroCountsWhenTcResultsMissing() {
        Map<String, Object> trace = baseTrace();

        ResultResponse res = ResultResponse.fromTrace(trace);

        assertThat(res.passCount()).isZero();
        assertThat(res.failCount()).isZero();
        assertThat(res.totalTcCount()).isZero();
    }

    @Test
    void preservesOtherFields() {
        Map<String, Object> trace = baseTrace();
        trace.put("trace_id", "abc123");
        trace.put("command", "test");
        trace.put("status", "completed");
        trace.put("started_at", "2026-05-19T00:00:00Z");
        trace.put("completed_at", "2026-05-19T00:01:00Z");

        ResultResponse res = ResultResponse.fromTrace(trace);

        assertThat(res.traceId()).isEqualTo("abc123");
        assertThat(res.command()).isEqualTo("test");
        assertThat(res.status()).isEqualTo("completed");
        assertThat(res.startedAt()).isEqualTo("2026-05-19T00:00:00Z");
        assertThat(res.completedAt()).isEqualTo("2026-05-19T00:01:00Z");
    }

    private Map<String, Object> baseTrace() {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("trace_id", "trace-1");
        trace.put("command", "test");
        trace.put("status", "completed");
        return trace;
    }
}
