package com.qapilot.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 인증 성공 응답 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record AuthResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        UserResponse user
) {
}
