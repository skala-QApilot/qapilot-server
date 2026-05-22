package com.qapilot.server.service.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 단일 GitHub/GitLab 저장소 설정.
 *
 * <p>{@link QapilotService#repos()} 안의 element. FastAPI GitCodebaseScannerTool 이
 * 기대하는 wire 포맷({@code repo_url/token/branch/role})과 1:1 매핑되도록 JsonProperty
 * 명명을 일치시킨다.
 *
 * <p>branch/role 은 null 허용 — FastAPI 가 default(main / URL-derived) 적용.
 *
 * <p>Author: C
 * <br>Created: 2026-05-22
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepoConfig(
        @JsonProperty("repo_url") String repoUrl,
        String token,
        String branch,
        String role
) {
}
