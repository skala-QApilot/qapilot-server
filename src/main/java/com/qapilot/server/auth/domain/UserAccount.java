package com.qapilot.server.auth.domain;

/**
 * 파일 기반 사용자 계정 모델.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record UserAccount(
        String userId,
        String email,
        String hashedPassword,
        String name,
        String role,
        String createdAt
) {
}
