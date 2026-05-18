package com.qapilot.server.auth.security;

/**
 * Spring Security 인증 principal.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record AuthenticatedUser(String userId, String email, String role) {
}
