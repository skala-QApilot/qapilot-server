package com.qapilot.server.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Agent 실행 시작 응답.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record AgentStartResponse(
        @JsonProperty("trace_id") String traceId,
        @JsonProperty("run_id") String runId,
        String status
) {
    public static AgentStartResponse running(String traceId) {
        return new AgentStartResponse(traceId, traceId, "running");
    }
}
