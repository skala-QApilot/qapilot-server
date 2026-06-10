package com.qapilot.server.run.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-07
 */
public interface ActionMappingRepository extends JpaRepository<ActionMappingEntity, UUID> {

    Optional<ActionMappingEntity> findFirstByServiceIdAndTcIdOrderByVersionDesc(UUID serviceId, String tcId);
}
