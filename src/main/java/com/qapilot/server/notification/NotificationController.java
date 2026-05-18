package com.qapilot.server.notification;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.notification.domain.Notification;
import com.qapilot.server.notification.dto.UpdateNotificationRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam("service_id") String serviceId,
            @RequestParam(value = "is_read", required = false) Boolean isRead
    ) {
        List<Notification> notifications = notificationService.list(serviceId, isRead);
        long unreadCount = notificationService.unreadCount(serviceId);
        return ApiResponse.ok(Map.of(
                "notifications", notifications,
                "count", notifications.size(),
                "unreadCount", unreadCount
        ));
    }

    @PatchMapping("/{notificationId}")
    public ApiResponse<Map<String, Notification>> update(
            @PathVariable String notificationId,
            @RequestParam("service_id") String serviceId,
            @RequestBody UpdateNotificationRequest request
    ) {
        Notification updated = notificationService.update(serviceId, notificationId, request.isRead());
        return ApiResponse.ok(Map.of("notification", updated));
    }
}
