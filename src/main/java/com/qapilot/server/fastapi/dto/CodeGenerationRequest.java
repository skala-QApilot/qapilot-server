package com.qapilot.server.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * FastAPI code-generation 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
public record CodeGenerationRequest(
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("qapilot_dir") String qapilotDir,
        @JsonProperty("scenario_ids") List<String> scenarioIds
) {
}
