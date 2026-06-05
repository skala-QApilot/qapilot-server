package com.qapilot.server.run.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
public interface TcArtifactRepository extends JpaRepository<TcArtifactEntity, UUID> {

    List<TcArtifactEntity> findAllByTcResultIdOrderByStepIndexAsc(UUID tcResultId);

    java.util.Optional<TcArtifactEntity> findFirstByTcResultIdAndStepIndexAndKind(
            UUID tcResultId, int stepIndex, String kind);
}
