package com.qapilot.server.run.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
public interface TcResultRepository extends JpaRepository<TcResultEntity, UUID> {

    List<TcResultEntity> findAllByRunId(UUID runId);

    long countByRunIdAndStatus(UUID runId, String status);

    java.util.Optional<TcResultEntity> findFirstByRunIdAndTsIdAndTcIdAndKind(
            UUID runId, String tsId, String tcId, String kind);
}
