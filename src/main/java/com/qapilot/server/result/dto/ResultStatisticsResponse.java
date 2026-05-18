package com.qapilot.server.result.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 결과 통계 응답.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ResultStatisticsResponse(
        int total,
        int passed,
        int failed,
        @JsonProperty("pass_rate") Double passRate
) {
}
