package com.qapilot.server.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * scenarios + scenario_versions 에 대한 write 어댑터. PR-15h.
 *
 * <p>UI 의 ScenarioService.create/update/delete 가 호출. 한 트랜잭션 안에서
 * scenario 행 + 새 version 행 + current_version_id 포인터 일괄 UPSERT.
 *
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
@Component
public class ScenarioWriter {

    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public ScenarioWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 시나리오 새 version 기록. scenarios 없으면 INSERT, 있으면 version_number++. */
    @Transactional
    public void upsertVersion(String serviceId, String tsId, Map<String, Object> payload) {
        UUID svc = UUID.fromString(serviceId);
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "시나리오 직렬화 실패");
        }

        // 1) scenarios UPSERT
        UUID newScenarioId = UUID.randomUUID();
        Object existingScenarioIdObj = em.createNativeQuery(
                "INSERT INTO scenarios (id, service_id, ts_id) " +
                "VALUES (CAST(:nid AS uuid), CAST(:svc AS uuid), :ts) " +
                "ON CONFLICT (service_id, ts_id) DO UPDATE SET updated_at = now() " +
                "RETURNING id::text"
        )
                .setParameter("nid", newScenarioId.toString())
                .setParameter("svc", svc.toString())
                .setParameter("ts", tsId)
                .getSingleResult();
        UUID scenarioId = UUID.fromString(String.valueOf(existingScenarioIdObj));

        // 2) next version
        Object maxObj = em.createNativeQuery(
                "SELECT COALESCE(MAX(version_number), 0) + 1 FROM scenario_versions WHERE scenario_id = CAST(:sid AS uuid)"
        ).setParameter("sid", scenarioId.toString()).getSingleResult();
        int nextVersion = ((Number) maxObj).intValue();

        // 3) INSERT version
        UUID versionId = UUID.randomUUID();
        em.createNativeQuery(
                "INSERT INTO scenario_versions (id, scenario_id, version_number, payload) " +
                "VALUES (CAST(:vid AS uuid), CAST(:sid AS uuid), :vn, CAST(:p AS jsonb))"
        )
                .setParameter("vid", versionId.toString())
                .setParameter("sid", scenarioId.toString())
                .setParameter("vn", nextVersion)
                .setParameter("p", payloadJson)
                .executeUpdate();

        // 4) UPDATE current_version_id
        em.createNativeQuery(
                "UPDATE scenarios SET current_version_id = CAST(:vid AS uuid), updated_at = now() WHERE id = CAST(:sid AS uuid)"
        )
                .setParameter("vid", versionId.toString())
                .setParameter("sid", scenarioId.toString())
                .executeUpdate();
    }

    @Transactional
    public void delete(String serviceId, String tsId) {
        em.createNativeQuery(
                "DELETE FROM scenarios WHERE service_id = CAST(:svc AS uuid) AND ts_id = :ts"
        )
                .setParameter("svc", serviceId)
                .setParameter("ts", tsId)
                .executeUpdate();
    }
}
