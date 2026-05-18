package com.qapilot.server.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qapilot.server.auth.domain.UserAccount;

/**
 * 사용자 응답 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record UserResponse(
        @JsonProperty("user_id") String userId,
        String email,
        String name,
        String role
) {
    public static UserResponse from(UserAccount user) {
        return new UserResponse(user.userId(), user.email(), user.name(), user.role());
    }
}
