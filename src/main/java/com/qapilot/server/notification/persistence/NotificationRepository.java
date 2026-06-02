package com.qapilot.server.notification.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findAllByServiceIdOrderByCreatedAtDesc(UUID serviceId);

    List<NotificationEntity> findAllByServiceIdAndIsReadOrderByCreatedAtDesc(UUID serviceId, boolean isRead);

    long countByServiceIdAndIsRead(UUID serviceId, boolean isRead);
}
