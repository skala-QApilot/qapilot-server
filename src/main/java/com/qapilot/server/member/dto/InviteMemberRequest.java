package com.qapilot.server.member.dto;

/**
 * 멤버 초대 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record InviteMemberRequest(
        String email,
        String name,
        String role,
        String team
) {
}
