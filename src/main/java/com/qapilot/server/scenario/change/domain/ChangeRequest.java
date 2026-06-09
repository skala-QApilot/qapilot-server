package com.qapilot.server.scenario.change.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 시나리오 변경 요청 도메인.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChangeRequest(
        String requestId,
        String scenarioId,
        String reason,
        String trigger,
        String status,
        String createdAt,
        String updatedAt,
        String reviewedAt,
        String reviewer,
        String targetId,
        String content
) {
}
