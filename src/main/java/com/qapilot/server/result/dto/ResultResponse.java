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
        Object tcResults = trace.get("tc_results");
        if (tcResults instanceof Map<?, ?> tcMap) {
            for (Object v : tcMap.values()) {
                if (v == null) continue;
                String s = v.toString();
                if ("passed".equals(s)) passCount++;
                else if ("failed".equals(s)) failCount++;
            }
        }
        int totalTcCount = tcResults instanceof Map<?, ?> tcMap2 ? tcMap2.size() : 0;
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
