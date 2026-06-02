package com.qapilot.server.member.dto;

import com.qapilot.server.auth.domain.UserAccount;
import com.qapilot.server.member.domain.Member;

/**
 * 멤버 응답 DTO (users.json join 포함).
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record MemberResponse(
        String memberId,
        String serviceId,
        String userId,
        String email,
        String name,
        String role,
        String team,
        String status,
        String joinedAt
) {

    public static MemberResponse of(Member member, UserAccount user) {
        return new MemberResponse(
                member.memberId(),
                member.serviceId(),
                member.userId(),
                user != null ? user.email() : null,
                user != null ? user.name() : null,
                member.role(),
                member.team(),
                member.status(),
                member.joinedAt()
        );
    }
}
