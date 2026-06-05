package com.qapilot.server.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qapilot.server.service.domain.RepoConfig;
import java.util.List;

/**
 * FastAPI scenario-generation 요청 DTO.
 *
 * <p>repos 가 채워져 있으면 FastAPI 파이프라인이 GitCodebaseScannerTool (GitHub/GitLab
 * REST API 스캔) 으로 분기한다. null 이면 로컬 디렉토리 스캔 fallback.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScenarioGenerationRequest(
        @JsonProperty("service_id") String serviceId,
        String trigger,
        @JsonProperty("user_input") String userInput,
        @JsonProperty("scenario_ids") List<String> scenarioIds,
        String filter,
        List<String> tags,
        List<RepoConfig> repos
) {
}
