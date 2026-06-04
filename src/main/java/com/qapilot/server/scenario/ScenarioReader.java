package com.qapilot.server.scenario;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.service.persistence.ServiceEntity;
import com.qapilot.server.service.persistence.ServiceJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * scenarios (통합 DB) → 기존 파일 기반 시나리오 Map 형태로 변환하는 read 어댑터.
 *
 * <p>V15 이후 scenarios 한 테이블에 (ts_id, version_number, payload, is_deleted) 가 모두 들어 있다.
 * latest = service_id 별 ts_id 그룹 안에서 version_number DESC 1 (is_deleted=false).
 *
 * <p>Author: C
 * <br>Created: 2026-06-02, V15 통합 적용 2026-06-04
 */
@Component
public class ScenarioReader {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final ServiceJpaRepository serviceJpaRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public ScenarioReader(ServiceJpaRepository serviceJpaRepository, ObjectMapper objectMapper) {
        this.serviceJpaRepository = serviceJpaRepository;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> listByServiceId(String serviceIdStr) {
        UUID serviceId = parseUuid(serviceIdStr);
        if (serviceId == null || serviceJpaRepository.findById(serviceId).isEmpty()) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT DISTINCT ON (ts_id) ts_id, payload::text " +
                "FROM scenarios " +
                "WHERE service_id = :svc AND is_deleted = false " +
                "ORDER BY ts_id, version_number DESC"
        ).setParameter("svc", serviceId).getResultList();

        List<Map<String, Object>> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Map<String, Object> map = decodePayload((String) row[1]);
            if (map == null) {
                map = new java.util.LinkedHashMap<>();
            }
            // payload 안에 ts_id 가 있을 수도/없을 수도 — 행의 ts_id 가 정답
            map.put("ts_id", row[0]);
            result.add(map);
        }
        return result;
    }

    public Optional<Map<String, Object>> findByServiceIdAndTsId(String serviceIdStr, String tsId) {
        UUID serviceId = parseUuid(serviceIdStr);
        if (serviceId == null) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT ts_id, payload::text " +
                "FROM scenarios " +
                "WHERE service_id = :svc AND ts_id = :ts AND is_deleted = false " +
                "ORDER BY version_number DESC LIMIT 1"
        ).setParameter("svc", serviceId).setParameter("ts", tsId).getResultList();

        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        Map<String, Object> map = decodePayload((String) row[1]);
        if (map == null) {
            map = new java.util.LinkedHashMap<>();
        }
        map.put("ts_id", row[0]);
        return Optional.of(map);
    }

    public Map<String, Object> requireByServiceIdAndTsId(String serviceIdStr, String tsId) {
        return findByServiceIdAndTsId(serviceIdStr, tsId)
                .orElseThrow(() -> new QapilotException(ErrorCode.SCENARIO_001));
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> decodePayload(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return null;
        }
    }

    // (linter 가 placeholder 미사용 경고 안 내도록 사용 — 향후 service 검증 hook 자리)
    @SuppressWarnings("unused")
    private ServiceEntity touch(ServiceEntity e) {
        return e;
    }
}
