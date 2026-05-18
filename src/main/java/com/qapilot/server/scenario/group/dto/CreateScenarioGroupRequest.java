package com.qapilot.server.scenario.group.dto;

import java.util.List;

/**
 * 시나리오 그룹 생성 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record CreateScenarioGroupRequest(String name, List<String> scenarioIds, List<String> tcIds) {
}
