package com.qapilot.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * access token 재발급 응답 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record AccessTokenResponse(@JsonProperty("access_token") String accessToken) {
}
