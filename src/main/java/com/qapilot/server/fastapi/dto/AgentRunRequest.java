package com.qapilot.server.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * FastAPI test-run 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentRunRequest(
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("qapilot_dir") String qapilotDir,
        @JsonProperty("scenario_ids") List<String> scenarioIds,
        String filter,
        List<String> tags,
        @JsonProperty("staging_url") String stagingUrl
) {
}
