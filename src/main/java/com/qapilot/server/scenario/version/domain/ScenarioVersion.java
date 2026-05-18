package com.qapilot.server.scenario.version.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * 시나리오 버전 스냅샷 도메인.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScenarioVersion(
        String versionId,
        String serviceId,
        String label,
        String description,
        List<Map<String, Object>> scenariosSnapshot,
        @JsonProperty("isFavorite") boolean isFavorite,
        String createdAt
) {
}
