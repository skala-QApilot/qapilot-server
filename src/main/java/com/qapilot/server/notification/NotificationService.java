package com.qapilot.server.notification;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.notification.domain.Notification;
import com.qapilot.server.notification.persistence.NotificationEntity;
import com.qapilot.server.notification.persistence.NotificationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 알림 관리 유스케이스. PR-15h — JPA only.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18, rewritten 2026-06-02
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> list(String serviceId, Boolean isRead) {
        UUID svc = UUID.fromString(serviceId);
        List<NotificationEntity> entities = isRead == null
                ? notificationRepository.findAllByServiceIdOrderByCreatedAtDesc(svc)
                : notificationRepository.findAllByServiceIdAndIsReadOrderByCreatedAtDesc(svc, isRead);
        return entities.stream().map(this::toDomain).toList();
    }

    public long unreadCount(String serviceId) {
        return notificationRepository.countByServiceIdAndIsRead(UUID.fromString(serviceId), false);
    }

    public Notification update(String serviceId, String notificationId, boolean isRead) {
        NotificationEntity entity;
        try {
            entity = notificationRepository.findById(UUID.fromString(notificationId))
                    .orElseThrow(() -> new QapilotException(ErrorCode.NOTIFICATION_001));
        } catch (IllegalArgumentException e) {
            throw new QapilotException(ErrorCode.NOTIFICATION_001);
        }
        if (!entity.getServiceId().equals(UUID.fromString(serviceId))) {
            throw new QapilotException(ErrorCode.NOTIFICATION_001);
        }
        if (isRead && !entity.isRead()) {
            entity.setReadAt(Instant.now());
        }
        entity.setRead(isRead);
        notificationRepository.save(entity);
        return toDomain(entity);
    }

    private Notification toDomain(NotificationEntity e) {
        return new Notification(
                e.getId().toString(),
                e.getTitle(),
                e.getMessage(),
                e.getType(),
                e.isRead(),
                e.getCreatedAt() == null ? null : e.getCreatedAt().toString(),
                e.getReadAt() == null ? null : e.getReadAt().toString()
        );
    }
}
