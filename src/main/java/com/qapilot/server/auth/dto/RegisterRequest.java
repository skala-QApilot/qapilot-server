package com.qapilot.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원가입 요청.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record RegisterRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String password,
        @JsonProperty("project_slug") String projectSlug,
        @JsonProperty("server_auth_token") String serverAuthToken
) {
}
