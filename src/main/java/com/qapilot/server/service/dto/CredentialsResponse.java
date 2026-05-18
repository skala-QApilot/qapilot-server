package com.qapilot.server.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qapilot.server.service.domain.QapilotService;

/**
 * 서비스 접속 정보 응답.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record CredentialsResponse(
        @JsonProperty("dashboard_url") String dashboardUrl,
        @JsonProperty("server_auth_token") String serverAuthToken,
        @JsonProperty("token_issued_at") String tokenIssuedAt,
        @JsonProperty("token_expires_at") String tokenExpiresAt
) {
    public static CredentialsResponse from(QapilotService service) {
        return new CredentialsResponse(
                service.dashboardUrl(),
                service.serverAuthToken(),
                service.tokenIssuedAt(),
                service.tokenExpiresAt()
        );
    }
}
