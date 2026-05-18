package com.qapilot.server.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qapilot.server.service.domain.QapilotService;

/**
 * 서비스 응답 DTO.
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
        @JsonProperty("updated_at") String updatedAt
) {
    public static ServiceResponse from(QapilotService service) {
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
                service.updatedAt()
        );
    }
}
