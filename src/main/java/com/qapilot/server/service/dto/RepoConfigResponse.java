package com.qapilot.server.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qapilot.server.service.domain.RepoConfig;

/**
 * 서비스 저장소 응답 DTO — UI 표시용.
 *
 * <p>PAT(token) 실제 값은 절대 직렬화하지 않고 존재 여부({@code token_set})만 노출한다.
 * 토큰 자체는 서버 → FastAPI(GitCodebaseScannerTool) 경로에서만 사용된다.
 *
 * <p>Author: C
 * <br>Created: 2026-06-05
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepoConfigResponse(
        @JsonProperty("repo_url") String repoUrl,
        String branch,
        String role,
        @JsonProperty("token_set") boolean tokenSet
) {
    public static RepoConfigResponse from(RepoConfig repo) {
        return new RepoConfigResponse(
                repo.repoUrl(),
                repo.branch(),
                repo.role(),
                repo.token() != null && !repo.token().isBlank()
        );
    }
}
