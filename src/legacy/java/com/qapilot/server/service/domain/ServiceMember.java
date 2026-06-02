package com.qapilot.server.service.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 서비스-사용자 멤버십 모델.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ServiceMember(
        @JsonProperty("member_id") String memberId,
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("user_id") String userId,
        String role,
        @JsonProperty("joined_at") String joinedAt
) {
}
