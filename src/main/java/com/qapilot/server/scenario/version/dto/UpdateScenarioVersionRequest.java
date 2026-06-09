package com.qapilot.server.scenario.version.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 시나리오 버전 수정 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record UpdateScenarioVersionRequest(
        @JsonProperty("isFavorite") Boolean isFavorite,
        String label,
        String description
) {
}
