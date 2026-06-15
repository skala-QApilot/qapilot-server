package com.qapilot.server.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 서비스 멤버 응답 — Slack 공유 대상 선택용 이메일 포함.
 *
 * <p>Author: C
 * <br>Created: 2026-06-15
 */
public record MemberResponse(
        @JsonProperty("user_id") String userId,
        String email,
        String name,
        String role
) {
}
