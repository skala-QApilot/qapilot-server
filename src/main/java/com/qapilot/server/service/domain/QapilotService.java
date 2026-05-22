package com.qapilot.server.service.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 파일 기반 서비스 메타데이터 모델.
 *
 * <p>repoUrl/repoToken/branch 는 GitCodebaseScannerTool 이 GitHub/GitLab REST API 로
 * 사용자 레포를 스캔할 때 사용한다. SaaS 모델에선 서버측에서만 보관해야 하며
 * UI/외부 응답으로 절대 노출하지 않는다. MVP 단계에선 평문 보관 — Phase D
 * DB 마이그레이션 시 암호화 컬럼으로 이동.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record QapilotService(
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("project_slug") String projectSlug,
        @JsonProperty("display_name") String displayName,
        String description,
        @JsonProperty("target_root") String targetRoot,
        @JsonProperty("qapilot_dir") String qapilotDir,
        @JsonProperty("dashboard_url") String dashboardUrl,
        @JsonProperty("server_auth_token") String serverAuthToken,
        @JsonProperty("token_issued_at") String tokenIssuedAt,
        @JsonProperty("token_expires_at") String tokenExpiresAt,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("repo_url") String repoUrl,
        @JsonProperty("repo_token") String repoToken,
        @JsonProperty("repo_branch") String repoBranch
) {
}
