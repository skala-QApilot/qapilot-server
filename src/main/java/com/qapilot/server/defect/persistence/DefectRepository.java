package com.qapilot.server.defect.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-04
 */
public interface DefectRepository extends JpaRepository<DefectEntity, UUID> {

    List<DefectEntity> findAllByServiceIdOrderByCreatedAtDesc(UUID serviceId);

    List<DefectEntity> findAllByServiceIdAndStatusOrderByCreatedAtDesc(UUID serviceId, String status);

    List<DefectEntity> findAllByRunIdOrderByCreatedAtDesc(UUID runId);
}
