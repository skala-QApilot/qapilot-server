package com.qapilot.server.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qapilot.server.service.domain.RepoConfig;
import java.util.List;

/**
 * FastAPI defect github-issue 생성 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-06-15
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateGithubIssueRequest(
        @JsonProperty("service_id") String serviceId,
        List<RepoConfig> repos
) {
}
