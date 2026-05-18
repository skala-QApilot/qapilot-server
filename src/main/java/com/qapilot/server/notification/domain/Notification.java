package com.qapilot.server.notification.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 알림 모델.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Notification(
        String notificationId,
        String title,
        String message,
        String type,
        @JsonProperty("isRead") boolean isRead,
        String createdAt,
        String readAt
) {
}
