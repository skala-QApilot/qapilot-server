package com.qapilot.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * access token 재발급 요청.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record RefreshRequest(@JsonProperty("refresh_token") @NotBlank String refreshToken) {
}
