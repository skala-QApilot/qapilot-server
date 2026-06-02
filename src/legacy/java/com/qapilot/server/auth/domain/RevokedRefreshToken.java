package com.qapilot.server.auth.domain;

/**
 * 무효화된 refresh token 식별자.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record RevokedRefreshToken(String jti, String expiresAt) {
}
