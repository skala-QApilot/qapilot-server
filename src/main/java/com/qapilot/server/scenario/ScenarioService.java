package com.qapilot.server.scenario;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.scenario.store.ScenarioFileStore;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 서비스 범위 시나리오 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class ScenarioService {

    private static final List<String> REQUIRED_FIELDS = List.of(
            "ts_id", "name", "description", "trigger", "affected_files", "domain_rules_used", "test_cases"
    );

    private final ServiceDomainService serviceDomainService;
    private final ScenarioFileStore scenarioFileStore;       // 잔존 — create/update/delete 의 file 쓰기 (PR-15g 에서 정리)
    private final ScenarioReader scenarioReader;             // PR-15d — 모든 read 의 single source
    private final ScenarioStatusAggregator statusAggregator;
    private final ScenarioPendingChangesResolver pendingChangesResolver;

    public ScenarioService(
            ServiceDomainService serviceDomainService,
            ScenarioFileStore scenarioFileStore,
            ScenarioReader scenarioReader,
            ScenarioStatusAggregator statusAggregator,
            ScenarioPendingChangesResolver pendingChangesResolver
    ) {
        this.serviceDomainService = serviceDomainService;
        this.scenarioFileStore = scenarioFileStore;
        this.scenarioReader = scenarioReader;
        this.statusAggregator = statusAggregator;
        this.pendingChangesResolver = pendingChangesResolver;
    }

    public List<Map<String, Object>> list(String serviceId, String search, String trigger) {
        Path qapilotDir = qapilotDir(serviceId);
        Map<String, ScenarioStatusAggregator.RunStatus> scenarioStatuses =
                statusAggregator.scenarioStatuses(qapilotDir);
        Set<String> pendingScenarioIds = pendingChangesResolver.scenariosWithPendingChanges(qapilotDir);

        List<Map<String, Object>> result = new ArrayList<>();
        // PR-15d — file → DB read. ScenarioReader 가 scenarios + scenario_versions JOIN 후 current 버전 payload 반환.
        for (Map<String, Object> scenario : scenarioReader.listByServiceId(serviceId)) {
            if (!matchesSearch(scenario, search)) continue;
            if (trigger != null && !trigger.isBlank() && !trigger.equals(scenario.get("trigger"))) continue;
            result.add(enrichScenario(scenario, scenarioStatuses, pendingScenarioIds));
        }
        return result;
    }

    public Map<String, Object> create(String serviceId, Map<String, Object> scenario) {
        validateScenario(scenario);
        scenarioFileStore.save(qapilotDir(serviceId), scenario);
        return scenario;
    }

    public Map<String, Object> get(String serviceId, String scenarioId) {
        Path qapilotDir = qapilotDir(serviceId);
        Map<String, Object> scenario = scenarioReader.requireByServiceIdAndTsId(serviceId, scenarioId);
        return enrichScenario(
                scenario,
                statusAggregator.scenarioStatuses(qapilotDir),
                pendingChangesResolver.scenariosWithPendingChanges(qapilotDir)
        );
    }

    public Map<String, Object> update(String serviceId, String scenarioId, Map<String, Object> patch) {
        Path qapilotDir = qapilotDir(serviceId);
        // enrich 필드가 디스크에 새지 않도록 raw 시나리오를 base 로 사용
        Map<String, Object> current = new LinkedHashMap<>(scenarioFileStore.load(qapilotDir, scenarioId));
        current.putAll(patch);
        current.put("ts_id", scenarioId);
        // 클라이언트가 실수로 보낸 derive 필드는 제거 (서버가 매번 재계산)
        current.remove("last_run_status");
        current.remove("last_run_at");
        current.remove("has_pending_changes");
        scenarioFileStore.save(qapilotDir, current);
        return enrichScenario(
                current,
                statusAggregator.scenarioStatuses(qapilotDir),
                pendingChangesResolver.scenariosWithPendingChanges(qapilotDir)
        );
    }

    public void delete(String serviceId, String scenarioId) {
        scenarioFileStore.delete(qapilotDir(serviceId), scenarioId);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> testCases(String serviceId, String scenarioId) {
        Path qapilotDir = qapilotDir(serviceId);
        // PR-15d — DB current 버전 payload 의 test_cases 만 추출
        Map<String, Object> raw = scenarioReader.requireByServiceIdAndTsId(serviceId, scenarioId);
        Object value = raw.get("test_cases");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        Map<String, ScenarioStatusAggregator.RunStatus> tcStatuses =
                statusAggregator.testCaseStatuses(qapilotDir);
        List<Map<String, Object>> enriched = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                enriched.add(enrichTestCase((Map<String, Object>) map, tcStatuses));
            }
        }
        return enriched;
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
    }

    /** Scenario raw map 에 last_run_status / last_run_at / has_pending_changes 를 추가한다. */
    private Map<String, Object> enrichScenario(
            Map<String, Object> scenario,
            Map<String, ScenarioStatusAggregator.RunStatus> scenarioStatuses,
            Set<String> pendingScenarioIds
    ) {
        Map<String, Object> enriched = new LinkedHashMap<>(scenario);
        String tsId = scenario.get("ts_id") == null ? null : scenario.get("ts_id").toString();
        ScenarioStatusAggregator.RunStatus status = tsId == null ? null : scenarioStatuses.get(tsId);
        enriched.put("last_run_status", status == null ? null : status.status());
        enriched.put("last_run_at", status == null ? null : status.at());
        enriched.put("has_pending_changes", tsId != null && pendingScenarioIds.contains(tsId));
        return enriched;
    }

    /** TestCase raw map 에 last_run_status / last_run_at 를 추가한다. */
    private Map<String, Object> enrichTestCase(
            Map<String, Object> testCase,
            Map<String, ScenarioStatusAggregator.RunStatus> tcStatuses
    ) {
        Map<String, Object> enriched = new LinkedHashMap<>(testCase);
        String tcId = testCase.get("tc_id") == null ? null : testCase.get("tc_id").toString();
        ScenarioStatusAggregator.RunStatus status = tcId == null ? null : tcStatuses.get(tcId);
        enriched.put("last_run_status", status == null ? null : status.status());
        enriched.put("last_run_at", status == null ? null : status.at());
        return enriched;
    }

    private void validateScenario(Map<String, Object> scenario) {
        List<String> missing = REQUIRED_FIELDS.stream()
                .filter(field -> !scenario.containsKey(field) || scenario.get(field) == null)
                .toList();
        if (!missing.isEmpty()) {
            throw new QapilotException(ErrorCode.COMMON_001, "필수 필드가 누락되었습니다: " + missing);
        }
    }

    private boolean matchesSearch(Map<String, Object> scenario, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String keyword = search.toLowerCase();
        return stringValue(scenario.get("name")).toLowerCase().contains(keyword)
                || stringValue(scenario.get("description")).toLowerCase().contains(keyword);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
