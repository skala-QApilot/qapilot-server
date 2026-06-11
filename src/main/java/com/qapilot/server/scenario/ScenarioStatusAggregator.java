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
        // verdict 일관화: cross_check kind 우선 (UI/API/DB 정합 + 의도 판정),
        // ui kind 는 fallback. 단 skip 보호 — UI 가 검증을 안 한 TC (skip) 를
        // cross_check 의 무신호 pass 가 통과로 둔갑시키지 않는다 (RunReader 와 동일 규칙).
        List<Object[]> rows = em.createNativeQuery(
                "SELECT DISTINCT ON (tc.tc_id, tc.kind) tc.tc_id, tc.kind, tc.status, r.completed_at, r.started_at " +
                "FROM tc_results tc JOIN runs r ON r.id = tc.run_id " +
                "WHERE r.service_id = :svc AND tc.kind IN ('ui','cross_check') AND tc.status IS NOT NULL " +
                "ORDER BY tc.tc_id, tc.kind, r.completed_at DESC NULLS LAST, r.started_at DESC"
        ).setParameter("svc", serviceId).getResultList();

        Map<String, RunStatus> result = new HashMap<>();
        Map<String, RunStatus> ccByTc = new HashMap<>();
        for (Object[] row : rows) {
            String tcId = (String) row[0];
            String kind = (String) row[1];
            String legacy = toLegacyStatus((String) row[2]);
            String at = toIso((Instant) (row[3] != null ? row[3] : row[4]));
            if ("ui".equals(kind)) {
                result.put(tcId, new RunStatus(legacy, at));
            } else {
                ccByTc.put(tcId, new RunStatus(legacy, at));
            }
        }
        for (Map.Entry<String, RunStatus> e : ccByTc.entrySet()) {
            RunStatus ui = result.get(e.getKey());
            boolean uiSkipped = ui != null && "skipped".equals(ui.status());
            if (uiSkipped && "passed".equals(e.getValue().status())) {
                continue; // skip 보호
            }
            result.put(e.getKey(), e.getValue());
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
        // TC verdict (cc 우선 + skip 보호) 를 ts 단위로 집계 — 화면 간 판정 일관성.
        Map<String, RunStatus> tcVerdicts = testCaseStatusesByServiceId(serviceId);

        Map<String, boolean[]> agg = new HashMap<>(); // ts → [anyFail, allPass]
        Map<String, String> whenAt = new HashMap<>();
        for (Map.Entry<String, RunStatus> e : tcVerdicts.entrySet()) {
            String tsId = tsFromTc(e.getKey());
            if (tsId.isEmpty()) continue;
            boolean[] a = agg.computeIfAbsent(tsId, k -> new boolean[]{false, true});
            String st = e.getValue().status();
            if ("failed".equals(st)) a[0] = true;
            if (!"passed".equals(st)) a[1] = false;
            String at = e.getValue().at();
            String prev = whenAt.get(tsId);
            if (at != null && (prev == null || at.compareTo(prev) > 0)) whenAt.put(tsId, at);
        }
        Map<String, RunStatus> result = new HashMap<>(agg.size());
        for (Map.Entry<String, boolean[]> e : agg.entrySet()) {
            String status = e.getValue()[0] ? "failed" : (e.getValue()[1] ? "passed" : null);
            if (status != null) {
                result.put(e.getKey(), new RunStatus(status, whenAt.get(e.getKey())));
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

    private static String tsFromTc(String tcId) {
        int i = tcId == null ? -1 : tcId.indexOf("-TC-");
        return i > 0 ? tcId.substring(0, i) : "";
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
