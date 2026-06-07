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
 * scenarios (통합 테이블) 에 대한 write 어댑터.
 *
 * <p>V15 이후 자동 작업 이력은 scenarios 한 테이블에 (ts_id, version_number, payload) row 누적으로 표현.
 * upsertVersion = MAX(version_number)+1 로 새 row INSERT.
 * delete = soft delete (해당 ts_id 의 모든 row 에 is_deleted=true). 이력 보존 + tc_results 등 외부 string 참조와 일관성.
 *
 * <p>Author: C
 * <br>Created: 2026-06-02, V15 통합 적용 2026-06-04
 */
@Component
public class ScenarioWriter {

    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public ScenarioWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 시나리오 새 version 기록. ts 가 처음이면 version_number=1, 아니면 MAX+1. */
    @Transactional
    public void upsertVersion(String serviceId, String tsId, Map<String, Object> payload) {
        UUID svc = UUID.fromString(serviceId);
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "시나리오 직렬화 실패");
        }

        em.createNativeQuery(
                "INSERT INTO scenarios (service_id, ts_id, version_number, payload) " +
                "VALUES (CAST(:svc AS uuid), :ts, " +
                "        COALESCE((SELECT MAX(version_number) FROM scenarios " +
                "                  WHERE service_id = CAST(:svc AS uuid) AND ts_id = :ts), 0) + 1, " +
                "        CAST(:p AS jsonb))"
        )
                .setParameter("svc", svc.toString())
                .setParameter("ts", tsId)
                .setParameter("p", payloadJson)
                .executeUpdate();
    }

    /** Soft delete — 해당 ts_id 의 모든 row 에 is_deleted=true. 이력 보존. */
    @Transactional
    public void delete(String serviceId, String tsId) {
        em.createNativeQuery(
                "UPDATE scenarios SET is_deleted = true " +
                "WHERE service_id = CAST(:svc AS uuid) AND ts_id = :ts"
        )
                .setParameter("svc", serviceId)
                .setParameter("ts", tsId)
                .executeUpdate();
    }

    /**
     * AI 수정 거절 시 호출 — V15 스키마 기준.
     *
     * <p>scenarios 테이블은 (service_id, ts_id, version_number) 로 이력을 관리한다.
     * 버전이 1개면 is_deleted=true 로 마킹(신규 생성 취소),
     * 2개 이상이면 최신 버전만 is_deleted=true 로 마킹해 직전 버전으로 롤백.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void rollbackOrDelete(String serviceId, String tsId) {
        Object maxVerObj = em.createNativeQuery(
                "SELECT MAX(version_number) FROM scenarios " +
                "WHERE service_id = CAST(:svc AS uuid) AND ts_id = :ts AND is_deleted = false"
        )
                .setParameter("svc", serviceId)
                .setParameter("ts", tsId)
                .getSingleResult();

        if (maxVerObj == null) {
            return;
        }

        int maxVersion = ((Number) maxVerObj).intValue();

        // 최신 버전 is_deleted = true — 버전 1개면 논리 삭제(취소), 2개 이상이면 롤백 효과
        em.createNativeQuery(
                "UPDATE scenarios SET is_deleted = true " +
                "WHERE service_id = CAST(:svc AS uuid) AND ts_id = :ts AND version_number = :vn"
        )
                .setParameter("svc", serviceId)
                .setParameter("ts", tsId)
                .setParameter("vn", maxVersion)
                .executeUpdate();
    }
}
