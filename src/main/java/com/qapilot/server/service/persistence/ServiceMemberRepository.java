package com.qapilot.server.service.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
public interface ServiceMemberRepository
        extends JpaRepository<ServiceMemberEntity, ServiceMemberEntity.PK> {

    boolean existsByServiceIdAndUserId(UUID serviceId, UUID userId);

    List<ServiceMemberEntity> findByServiceId(UUID serviceId);
}
