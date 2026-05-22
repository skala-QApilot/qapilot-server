package com.qapilot.server.service.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 파일 기반 서비스 메타데이터 모델.
 *
 * <p>repos / stagingUrl 은 SaaS 통합용 — UI ServiceSetupPage 에서 입력한 GitHub
 * 저장소 목록과 스테이징 서버 URL 을 보관한다. SaaS 모델에선 서버측에서만 보관해야
 * 하며 UI/외부 응답으로 절대 노출하지 않는다. MVP 단계 파일 보관 — Phase D DB
 * 마이그레이션 시 암호화 컬럼/테이블로 이동.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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
        @JsonProperty("repos") List<RepoConfig> repos,
        @JsonProperty("staging_url") String stagingUrl
) {
}
