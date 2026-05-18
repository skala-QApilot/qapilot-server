package com.qapilot.server.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 알림 수정 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record UpdateNotificationRequest(
        @JsonProperty("isRead") boolean isRead
) {
}
