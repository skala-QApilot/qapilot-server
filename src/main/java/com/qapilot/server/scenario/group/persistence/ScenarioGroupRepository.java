package com.qapilot.server.scenario.group.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
public interface ScenarioGroupRepository extends JpaRepository<ScenarioGroupEntity, UUID> {

    List<ScenarioGroupEntity> findAllByServiceIdOrderByCreatedAtDesc(UUID serviceId);
}
