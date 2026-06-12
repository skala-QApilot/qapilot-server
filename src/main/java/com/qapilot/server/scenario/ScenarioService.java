package com.qapilot.server.scenario;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.fastapi.FastApiAgentClient;
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
 * 서비스 범위 시나리오 유스케이스. PR-15h — JPA only.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18, rewritten 2026-06-02
 */
@Service
public class ScenarioService {

    private static final List<String> REQUIRED_FIELDS = List.of(
            "ts_id", "name", "description", "trigger", "affected_files", "domain_rules_used", "test_cases"
    );

    private final ServiceDomainService serviceDomainService;
    private final ScenarioReader scenarioReader;
    private final ScenarioWriter scenarioWriter;
    private final ScenarioStatusAggregator statusAggregator;
    private final ScenarioPendingChangesResolver pendingChangesResolver;
    private final FastApiAgentClient fastApiAgentClient;

    public ScenarioService(
            ServiceDomainService serviceDomainService,
            ScenarioReader scenarioReader,
            ScenarioWriter scenarioWriter,
            ScenarioStatusAggregator statusAggregator,
            ScenarioPendingChangesResolver pendingChangesResolver,
            FastApiAgentClient fastApiAgentClient
    ) {
        this.serviceDomainService = serviceDomainService;
        this.scenarioReader = scenarioReader;
        this.scenarioWriter = scenarioWriter;
        this.statusAggregator = statusAggregator;
        this.pendingChangesResolver = pendingChangesResolver;
        this.fastApiAgentClient = fastApiAgentClient;
    }

    public List<Map<String, Object>> list(String serviceId, String search, String trigger) {
        Path qapilotDir = qapilotDir(serviceId);
        Map<String, ScenarioStatusAggregator.RunStatus> scenarioStatuses =
                statusAggregator.scenarioStatuses(qapilotDir);
        Set<String> pendingScenarioIds = pendingChangesResolver.scenariosWithPendingChanges(qapilotDir);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> scenario : scenarioReader.listByServiceId(serviceId)) {
            if (!matchesSearch(scenario, search)) continue;
            if (trigger != null && !trigger.isBlank() && !trigger.equals(scenario.get("trigger"))) continue;
            result.add(enrichScenario(scenario, scenarioStatuses, pendingScenarioIds));
        }
        return result;
    }

    public Map<String, Object> create(String serviceId, Map<String, Object> scenario) {
        validateScenario(scenario);
        String tsId = String.valueOf(scenario.get("ts_id"));
        scenarioWriter.upsertVersion(serviceId, tsId, scenario);
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
        Map<String, Object> current = new LinkedHashMap<>(
                scenarioReader.requireByServiceIdAndTsId(serviceId, scenarioId));
        current.putAll(patch);
        current.put("ts_id", scenarioId);
        current.remove("last_run_status");
        current.remove("last_run_at");
        current.remove("has_pending_changes");
        scenarioWriter.upsertVersion(serviceId, scenarioId, current);
        return enrichScenario(
                current,
                statusAggregator.scenarioStatuses(qapilotDir),
                pendingChangesResolver.scenariosWithPendingChanges(qapilotDir)
        );
    }

    public void delete(String serviceId, String scenarioId) {
        scenarioWriter.delete(serviceId, scenarioId);
    }

    /** 도메인 문서(PRD 등) 원문. 없으면 SCENARIO_003. */
    public byte[] documentContent(String serviceId, String filename) {
        byte[] content = fastApiAgentClient.documentContent(serviceId, filename);
        if (content == null) {
            throw new QapilotException(ErrorCode.SCENARIO_003);
        }
        return content;
    }

    /** codebase_ref 의 코드 본문. 없으면 SCENARIO_004. */
    public Map<String, Object> sourceContent(String serviceId, String file, String commitSha, Integer lineStart, Integer lineEnd) {
        Map<String, Object> content = fastApiAgentClient.sourceContent(serviceId, file, commitSha, lineStart, lineEnd);
        if (content == null) {
            throw new QapilotException(ErrorCode.SCENARIO_004);
        }
        return content;
    }

    /** TC 의 최신 action mapping (실행 스텝 시퀀스). 없으면 SCENARIO_005. */
    public Map<String, Object> actionMapping(String serviceId, String tcId) {
        Map<String, Object> mapping = fastApiAgentClient.actionMapping(serviceId, tcId);
        if (mapping == null) {
            throw new QapilotException(ErrorCode.SCENARIO_005);
        }
        return mapping;
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> testCases(String serviceId, String scenarioId) {
        Path qapilotDir = qapilotDir(serviceId);
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
