package com.qapilot.server.scenario.change.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
public interface ChangeRequestRepository extends JpaRepository<ChangeRequestEntity, UUID> {

    /** content(jsonb)를 text로 캐스팅해서 Hibernate String 매핑 오류를 회피한다. */
    @Query(nativeQuery = true, value = """
            SELECT id, service_id, scenario_id, reason, target_id,
                   CAST(content AS TEXT) AS content,
                   trigger, status, reviewer, reviewed_at, created_at, updated_at
            FROM change_requests
            WHERE service_id = :serviceId
            ORDER BY created_at DESC
            """)
    List<ChangeRequestEntity> findAllByServiceIdOrderByCreatedAtDesc(@Param("serviceId") UUID serviceId);

    List<ChangeRequestEntity> findAllByServiceIdAndScenarioIdOrderByCreatedAtDesc(UUID serviceId, String scenarioId);
}
