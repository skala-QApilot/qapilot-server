package com.qapilot.server.notification;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.notification.domain.Notification;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 알림 관리 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class NotificationService {

    private final ServiceDomainService serviceDomainService;
    private final NotificationFileStore notificationFileStore;

    public NotificationService(ServiceDomainService serviceDomainService, NotificationFileStore notificationFileStore) {
        this.serviceDomainService = serviceDomainService;
        this.notificationFileStore = notificationFileStore;
    }

    public List<Notification> list(String serviceId, Boolean isRead) {
        Path qapilotDir = qapilotDir(serviceId);
        List<Notification> all = notificationFileStore.load(qapilotDir);
        if (isRead == null) {
            return all;
        }
        return all.stream().filter(n -> n.isRead() == isRead).toList();
    }

    public long unreadCount(String serviceId) {
        Path qapilotDir = qapilotDir(serviceId);
        return notificationFileStore.load(qapilotDir).stream().filter(n -> !n.isRead()).count();
    }

    public Notification update(String serviceId, String notificationId, boolean isRead) {
        Path qapilotDir = qapilotDir(serviceId);
        List<Notification> notifications = notificationFileStore.load(qapilotDir);

        Notification current = notifications.stream()
                .filter(n -> notificationId.equals(n.notificationId()))
                .findFirst()
                .orElseThrow(() -> new QapilotException(ErrorCode.NOTIFICATION_001));

        String readAt = (isRead && !current.isRead()) ? Instant.now().toString() : current.readAt();

        Notification updated = new Notification(
                current.notificationId(),
                current.title(),
                current.message(),
                current.type(),
                isRead,
                current.createdAt(),
                readAt
        );

        List<Notification> replaced = notifications.stream()
                .map(n -> notificationId.equals(n.notificationId()) ? updated : n)
                .toList();
        notificationFileStore.save(qapilotDir, replaced);
        return updated;
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
    }
}
