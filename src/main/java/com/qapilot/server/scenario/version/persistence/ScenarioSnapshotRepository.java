package com.qapilot.server.scenario.version.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
public interface ScenarioSnapshotRepository extends JpaRepository<ScenarioSnapshotEntity, UUID> {

    List<ScenarioSnapshotEntity> findAllByServiceIdOrderByCreatedAtDesc(UUID serviceId);
}
