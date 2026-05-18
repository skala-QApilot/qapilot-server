package com.qapilot.server.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
}
