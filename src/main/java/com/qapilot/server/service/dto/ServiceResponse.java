package com.qapilot.server.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qapilot.server.service.domain.QapilotService;
import java.util.List;

/**
 * 서비스 응답 DTO.
 *
 * <p>repos 는 PAT 를 제외한 메타(repo_url/branch/role/token_set)만 노출한다 —
 * {@link RepoConfigResponse} 참고. staging_url 은 설정 화면 표시/수정용.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ServiceResponse(
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("project_slug") String projectSlug,
        @JsonProperty("display_name") String displayName,
        String description,
        @JsonProperty("target_root") String targetRoot,
        @JsonProperty("qapilot_dir") String qapilotDir,
        @JsonProperty("dashboard_url") String dashboardUrl,
        @JsonProperty("server_auth_token") String serverAuthToken,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("repos") List<RepoConfigResponse> repos,
        @JsonProperty("staging_url") String stagingUrl
) {
    public static ServiceResponse from(QapilotService service) {
        List<RepoConfigResponse> repos = service.repos() == null
                ? null
                : service.repos().stream().map(RepoConfigResponse::from).toList();
        return new ServiceResponse(
                service.serviceId(),
                service.projectSlug(),
                service.displayName(),
                service.description(),
                service.targetRoot(),
                service.qapilotDir(),
                service.dashboardUrl(),
                service.serverAuthToken(),
                service.createdAt(),
                service.updatedAt(),
                repos,
                service.stagingUrl()
        );
    }
}
