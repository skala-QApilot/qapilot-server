package com.qapilot.server.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 서비스 생성 요청.
 *
 * <p>repoUrl/repoToken/repoBranch 는 GitHub/GitLab 통합용 (선택). 입력 시 서비스
 * 메타에 저장되어 후속 Agent 실행(시나리오 생성 등)에서 자동 활용된다.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ServiceCreateRequest(
        String name,
        String description,
        @JsonProperty("target_root") String targetRoot,
        @JsonProperty("repo_url") String repoUrl,
        @JsonProperty("repo_token") String repoToken,
        @JsonProperty("repo_branch") String repoBranch
) {
}
