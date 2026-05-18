package com.qapilot.server.service.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 파일 기반 서비스 메타데이터 모델.
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
        @JsonProperty("updated_at") String updatedAt
) {
}
