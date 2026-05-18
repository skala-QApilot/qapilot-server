package com.qapilot.server.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 서비스 범위 시나리오 생성 시작 요청.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ScenarioGenerationStartRequest(
        String trigger,
        @JsonProperty("user_input") String userInput,
        @JsonProperty("scenario_ids") List<String> scenarioIds,
        String filter,
        List<String> tags
) {
}
