package com.qapilot.server.member.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 서비스 멤버십 모델.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Member(
        @JsonProperty("member_id") String memberId,
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("user_id") String userId,
        String role,
        String team,
        String status,
        @JsonProperty("joined_at") String joinedAt
) {
}
