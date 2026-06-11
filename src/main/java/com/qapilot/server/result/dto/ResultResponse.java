package com.qapilot.server.result.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * trace 기반 결과 응답.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ResultResponse(
        @JsonProperty("trace_id") String traceId,
        String command,
        String status,
        @JsonProperty("started_at") String startedAt,
        @JsonProperty("completed_at") String completedAt,
        String error,
        Double confidence,
        @JsonProperty("total_cost") double totalCost,
        @JsonProperty("result_summary") Map<String, Object> resultSummary,
        @JsonProperty("pass_count") int passCount,
        @JsonProperty("fail_count") int failCount,
        @JsonProperty("skip_count") int skipCount,
        @JsonProperty("unverified_count") int unverifiedCount,
        @JsonProperty("total_tc_count") int totalTcCount
) {
    @SuppressWarnings("unchecked")
    public static ResultResponse fromTrace(Map<String, Object> trace) {
        Object summary = trace.get("result_summary");
        Map<String, Object> resultSummary = summary instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        int passCount = 0;
        int failCount = 0;
        int skipCount = 0;
        int unverifiedCount = 0;
        Object tcResults = trace.get("tc_results");
        if (tcResults instanceof Map<?, ?> tcMap) {
            for (Object v : tcMap.values()) {
                if (v == null) continue;
                String s = v.toString();
                if ("passed".equals(s)) passCount++;
                else if ("failed".equals(s)) failCount++;
                // "skipped" = 실행됐지만 검증 미완 (자동화 불가 step 보유 등) —
                // 미실행(N) 과 의미가 다르므로 별도 집계 (UI 의 S 표시용)
                else if ("skipped".equals(s)) skipCount++;
                // "unverified" = cross_check 가 API/DB 검증 부재로 판정 보류 (U)
                else if ("unverified".equals(s)) unverifiedCount++;
            }
        }
        // total_tc_count = "사용자가 선택한 시나리오들의 전체 TC 수" — pipeline 의 _load_scenarios_for_test
        // 가 trace.json 에 selected_total_tc_count 로 박아둠. 옛 trace 호환을 위해 tc_results.size() fallback.
        Object selectedTotal = trace.get("selected_total_tc_count");
        int totalTcCount = selectedTotal instanceof Number numTotal
                ? numTotal.intValue()
                : (tcResults instanceof Map<?, ?> tcMap2 ? tcMap2.size() : 0);
        return new ResultResponse(
                stringValue(trace.get("trace_id")),
                stringValue(trace.get("command")),
                stringValue(trace.get("status")),
                stringValue(trace.get("started_at")),
                stringValue(trace.get("completed_at")),
                nullableString(trace.get("error")),
                doubleOrNull(trace.get("confidence")),
                doubleValue(trace.get("total_cost")),
                resultSummary,
                passCount,
                failCount,
                skipCount,
                unverifiedCount,
                totalTcCount
        );
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String nullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private static Double doubleOrNull(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }
}
