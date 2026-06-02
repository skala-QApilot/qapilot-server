package com.qapilot.server.rtm;

import com.qapilot.server.rtm.domain.RtmRequirement;
import com.qapilot.server.rtm.domain.RtmSummary;
import com.qapilot.server.rtm.domain.RtmVersion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * rtm_versions + rtm_requirements + rtm_requirement_tc_links (DB) → RtmVersion 도메인 객체로 변환.
 *
 * <p>PR-15e — RtmFileStore.listAll/load 의 file read 를 DB read 로 대체.
 * status/passCount 등 동적 필드는 비워서 반환 (RtmService.enrichVersion 이 응답 시점 derive).
 *
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
@Component
public class RtmReader {

    @PersistenceContext
    private EntityManager em;

    public List<RtmVersion> listByServiceId(String serviceIdStr) {
        UUID serviceId = parseUuid(serviceIdStr);
        if (serviceId == null) {
            return List.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> versionRows = em.createNativeQuery(
                "SELECT id::text, label, trace_id::text, created_at::text " +
                "FROM rtm_versions WHERE service_id = :svc " +
                "ORDER BY created_at DESC"
        ).setParameter("svc", serviceId).getResultList();

        List<RtmVersion> result = new ArrayList<>(versionRows.size());
        for (Object[] row : versionRows) {
            String versionId = (String) row[0];
            String label = (String) row[1];
            String traceId = (String) row[2];
            String createdAt = (String) row[3];
            List<RtmRequirement> reqs = loadRequirementsForVersion(UUID.fromString(versionId));
            result.add(new RtmVersion(versionId, serviceIdStr, label, traceId, reqs,
                    emptySummary(reqs.size()), createdAt));
        }
        return result;
    }

    public Optional<RtmVersion> findById(String serviceIdStr, String rtmVersionIdStr) {
        UUID serviceId = parseUuid(serviceIdStr);
        UUID versionId = parseUuid(rtmVersionIdStr);
        if (serviceId == null || versionId == null) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT label, trace_id::text, created_at::text " +
                "FROM rtm_versions WHERE id = :vid AND service_id = :svc"
        ).setParameter("vid", versionId).setParameter("svc", serviceId).getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        List<RtmRequirement> reqs = loadRequirementsForVersion(versionId);
        return Optional.of(new RtmVersion(rtmVersionIdStr, serviceIdStr,
                (String) row[0], (String) row[1], reqs,
                emptySummary(reqs.size()), (String) row[2]));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 내부 helper
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 한 rtm_version 의 requirements + 각 requirement 의 linked TC 들을 1 round-trip 으로 로드.
     * (N+1 회피 — LEFT JOIN 후 Java 측에서 grouping)
     */
    @SuppressWarnings("unchecked")
    private List<RtmRequirement> loadRequirementsForVersion(UUID versionId) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT r.id::text, r.req_id, r.content, l.tc_id " +
                "FROM rtm_requirements r " +
                "LEFT JOIN rtm_requirement_tc_links l ON l.rtm_requirement_id = r.id " +
                "WHERE r.rtm_version_id = :vid " +
                "ORDER BY r.req_id, l.tc_id"
        ).setParameter("vid", versionId).getResultList();

        Map<String, RtmRequirementBuilder> bucket = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String reqRowId = (String) row[0];
            String frId = (String) row[1];
            String content = (String) row[2];
            String tcId = (String) row[3];
            RtmRequirementBuilder b = bucket.computeIfAbsent(reqRowId, k -> new RtmRequirementBuilder(frId, content));
            if (tcId != null) {
                b.linkedTcIds.add(tcId);
            }
        }
        List<RtmRequirement> result = new ArrayList<>(bucket.size());
        for (RtmRequirementBuilder b : bucket.values()) {
            // status/passCount/totalCount/history 는 RtmService.enrichVersion 이 SQL 집계로 채운다.
            result.add(new RtmRequirement(
                    b.frId, b.content, "미측정", 0, b.linkedTcIds.size(), List.of(), b.linkedTcIds
            ));
        }
        return result;
    }

    private RtmSummary emptySummary(int total) {
        return new RtmSummary(total, 0, 0, total);
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static final class RtmRequirementBuilder {
        final String frId;
        final String content;
        final List<String> linkedTcIds = new ArrayList<>();

        RtmRequirementBuilder(String frId, String content) {
            this.frId = frId;
            this.content = content;
        }
    }
}
