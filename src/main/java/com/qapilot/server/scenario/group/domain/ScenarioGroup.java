package com.qapilot.server.scenario.group.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 시나리오 그룹 도메인.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScenarioGroup(
        String groupId,
        String name,
        List<String> scenarioIds,
        List<String> tcIds,
        Schedule schedule,
        String createdAt,
        String updatedAt
) {
}
