package com.qapilot.server.scenario.version.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * scenario_versions (사용자 마일스톤) 리포지토리. PR V15.
 *
 * <p>Author: C
 * <br>Created: 2026-06-02, renamed 2026-06-04
 */
public interface ScenarioVersionRepository extends JpaRepository<ScenarioVersionEntity, UUID> {

    List<ScenarioVersionEntity> findAllByServiceIdOrderByCreatedAtDesc(UUID serviceId);
}
