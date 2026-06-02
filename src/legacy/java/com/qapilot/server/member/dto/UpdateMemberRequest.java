package com.qapilot.server.member.dto;

/**
 * 멤버 수정 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record UpdateMemberRequest(
        String role,
        String team
) {
}
