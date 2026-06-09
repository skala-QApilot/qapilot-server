package com.qapilot.server.service.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
public interface ServiceRepoJpaRepository extends JpaRepository<ServiceRepoEntity, UUID> {

    List<ServiceRepoEntity> findAllByServiceIdOrderByPositionAsc(UUID serviceId);

    void deleteAllByServiceId(UUID serviceId);
}
