package com.qapilot.server.scenario;

import com.qapilot.server.service.persistence.ServiceJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 시나리오 / TC 별 last_run_status — tc_results JOIN runs 의 SQL 집계.
 *
 * <p>PR-15d — trace JSON 디스크 스캔 → SQL 전환. Path-기반 시그니처는 호환을 위해 유지하되
 * 실제로는 qapilotDir 의 마지막 segment (slug) 로 service 를 lookup → 그 service 의 runs 만 본다.
 *
 * <p>status 어휘는 기존 file 기반과 호환을 위해 매핑:
 * <ul>
 *   <li>tc_results.status = "pass"/"fail"/"skip" → "passed"/"failed"/"skipped"</li>
 *   <li>scenario 단위 status 는 TC 중 하나라도 fail 이면 "failed", 모두 pass 면 "passed"</li>
 * </ul>
 *
 * <p>Author: C
 * <br>Created: 2026-05-19, rewritten 2026-06-02
 */
@Component
public class ScenarioStatusAggregator {

    private final ServiceJpaRepository serviceJpaRepository;

    @PersistenceContext
    private EntityManager em;

    public ScenarioStatusAggregator(ServiceJpaRepository serviceJpaRepository) {
        this.serviceJpaRepository = serviceJpaRepository;
    }

    /** ts_id 별 최근 실행 status (entry 없으면 한 번도 실행 안 됨). */
    public Map<String, RunStatus> scenarioStatuses(Path qapilotDir) {
        UUID serviceId = serviceIdFromQapilotDir(qapilotDir);
        if (serviceId == null) {
            return Map.of();
        }
        return scenarioStatusesByServiceId(serviceId);
    }

    /** tc_id 별 최근 실행 status. */
    public Map<String, RunStatus> testCaseStatuses(Path qapilotDir) {
        UUID serviceId = serviceIdFromQapilotDir(qapilotDir);
        if (serviceId == null) {
            return Map.of();
        }
        return testCaseStatusesByServiceId(serviceId);
    }

    /**
     * tc_id 별 최근 ui kind 결과.
     *
     * <p>DISTINCT ON (tc_id) — 같은 TC 가 여러 run 에 걸쳐 결과 있으면 가장 최근 (completed_at DESC) 만.
     * api/db kind 는 supplementary 라 제외, ui status 가 TC 의 운명을 결정.
     */
    @SuppressWarnings("unchecked")
    public Map<String, RunStatus> testCaseStatusesByServiceId(UUID serviceId) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT DISTINCT ON (tc.tc_id) tc.tc_id, tc.status, r.completed_at, r.started_at " +
                "FROM tc_results tc JOIN runs r ON r.id = tc.run_id " +
                "WHERE r.service_id = :svc AND tc.kind = 'ui' AND tc.status IS NOT NULL " +
                "ORDER BY tc.tc_id, r.completed_at DESC NULLS LAST, r.started_at DESC"
        ).setParameter("svc", serviceId).getResultList();

        Map<String, RunStatus> result = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            String tcId = (String) row[0];
            String legacy = toLegacyStatus((String) row[1]);
            String at = toIso((Instant) (row[2] != null ? row[2] : row[3]));
            result.put(tcId, new RunStatus(legacy, at));
        }
        return result;
    }

    /**
     * ts_id 별 시나리오 단위 status — TC 결과들에서 derive.
     * 하나라도 fail 이면 failed, 전부 pass 면 passed, 그 외엔 (skip 만 있거나 mixed null) → null.
     */
    @SuppressWarnings("unchecked")
    public Map<String, RunStatus> scenarioStatusesByServiceId(UUID serviceId) {
        // 각 ts_id 의 최근 run 의 (status, completed_at) 들을 모은다.
        // ts_id 별로 그 run 의 tc 결과를 aggregate.
        List<Object[]> rows = em.createNativeQuery(
                "WITH latest_tc AS (" +
                "  SELECT DISTINCT ON (tc.ts_id, tc.tc_id) tc.ts_id, tc.tc_id, tc.status, " +
                "         r.completed_at, r.started_at " +
                "  FROM tc_results tc JOIN runs r ON r.id = tc.run_id " +
                "  WHERE r.service_id = :svc AND tc.kind = 'ui' AND tc.status IS NOT NULL " +
                "  ORDER BY tc.ts_id, tc.tc_id, r.completed_at DESC NULLS LAST, r.started_at DESC" +
                ") " +
                "SELECT ts_id, " +
                "       BOOL_OR(status = 'fail')  AS any_fail, " +
                "       BOOL_AND(status = 'pass') AS all_pass, " +
                "       MAX(COALESCE(completed_at, started_at)) AS when_at " +
                "FROM latest_tc GROUP BY ts_id"
        ).setParameter("svc", serviceId).getResultList();

        Map<String, RunStatus> result = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            String tsId = (String) row[0];
            Boolean anyFail = (Boolean) row[1];
            Boolean allPass = (Boolean) row[2];
            String at = toIso((Instant) row[3]);
            String status;
            if (Boolean.TRUE.equals(anyFail)) {
                status = "failed";
            } else if (Boolean.TRUE.equals(allPass)) {
                status = "passed";
            } else {
                status = null;  // skip 만 있거나 mixed — 의도적으로 미정
            }
            if (status != null) {
                result.put(tsId, new RunStatus(status, at));
            }
        }
        return result;
    }

    /**
     * qapilotDir (e.g. /...../system-under-test/.qapilot/test) 의 마지막 segment 가 service slug.
     * 그 slug → service UUID 변환. PR-15 컷오버 진행 중이라 qapilotDir 시그니처는 유지.
     */
    private UUID serviceIdFromQapilotDir(Path qapilotDir) {
        if (qapilotDir == null || qapilotDir.getFileName() == null) {
            return null;
        }
        String slug = qapilotDir.getFileName().toString();
        return serviceJpaRepository.findAll().stream()
                .filter(s -> slug.equals(s.getSlug()))
                .map(s -> s.getId())
                .findFirst()
                .orElse(null);
    }

    private static String toLegacyStatus(String dbStatus) {
        return switch (dbStatus) {
            case "pass" -> "passed";
            case "fail" -> "failed";
            case "skip" -> "skipped";
            default -> dbStatus;
        };
    }

    private static String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    public record RunStatus(String status, String at) {
    }
}
