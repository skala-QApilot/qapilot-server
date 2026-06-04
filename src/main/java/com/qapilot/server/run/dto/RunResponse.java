package com.qapilot.server.run.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * trace 기반 실행 응답.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record RunResponse(
        String id,
        String name,
        String status,
        @JsonProperty("startTime") String startTime,
        @JsonProperty("completedAt") String completedAt,
        String error,
        @JsonProperty("result_summary") Map<String, Object> resultSummary,
        @JsonProperty("scenario_ids") List<String> scenarioIds
) {
    @SuppressWarnings("unchecked")
    public static RunResponse fromTrace(Map<String, Object> trace) {
        String traceId = stringValue(trace.get("trace_id"));
        String command = stringValue(trace.get("command"));
        Object summary = trace.get("result_summary");
        Map<String, Object> resultSummary = summary instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        Object ids = trace.get("scenario_ids");
        List<String> scenarioIds = ids instanceof List<?> list
                ? list.stream().map(RunResponse::stringValue).toList()
                : null;
        return new RunResponse(
                traceId,
                command + " - " + traceId.substring(0, Math.min(8, traceId.length())),
                stringValue(trace.get("status")),
                stringValue(trace.get("started_at")),
                stringValue(trace.get("completed_at")),
                nullableString(trace.get("error")),
                resultSummary,
                scenarioIds
        );
    }

    public static RunResponse running(String traceId) {
        return new RunResponse(traceId, "test - " + traceId.substring(0, Math.min(8, traceId.length())),
                "running", null, null, null, Map.of(), null);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String nullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
