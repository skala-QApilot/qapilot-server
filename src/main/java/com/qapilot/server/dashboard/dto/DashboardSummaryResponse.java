package com.qapilot.server.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qapilot.server.run.dto.RunResponse;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 요약 응답.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record DashboardSummaryResponse(
        @JsonProperty("scenarios_count") int scenariosCount,
        @JsonProperty("recent_runs") List<RunResponse> recentRuns,
        @JsonProperty("domain_files") List<String> domainFiles,
        @JsonProperty("rtm_summary") Map<String, Object> rtmSummary,
        @JsonProperty("pass_rate") Double passRate
) {
}
