package com.qapilot.server.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * FastAPI scenario-generation 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ScenarioGenerationRequest(
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("qapilot_dir") String qapilotDir,
        String trigger,
        @JsonProperty("user_input") String userInput,
        @JsonProperty("scenario_ids") List<String> scenarioIds,
        String filter,
        List<String> tags
) {
}
