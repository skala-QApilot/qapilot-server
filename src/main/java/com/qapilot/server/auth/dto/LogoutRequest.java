package com.qapilot.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그아웃 요청.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record LogoutRequest(@JsonProperty("refresh_token") @NotBlank String refreshToken) {
}
