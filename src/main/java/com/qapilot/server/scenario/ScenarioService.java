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
    private final ScenarioFileStore scenarioFileStore;

    public ScenarioService(ServiceDomainService serviceDomainService, ScenarioFileStore scenarioFileStore) {
        this.serviceDomainService = serviceDomainService;
        this.scenarioFileStore = scenarioFileStore;
    }

    public List<Map<String, Object>> list(String serviceId, String search, String trigger) {
        Path qapilotDir = qapilotDir(serviceId);
        return scenarioFileStore.listAll(qapilotDir).stream()
                .filter(scenario -> matchesSearch(scenario, search))
                .filter(scenario -> trigger == null || trigger.isBlank() || trigger.equals(scenario.get("trigger")))
                .toList();
    }

    public Map<String, Object> create(String serviceId, Map<String, Object> scenario) {
        validateScenario(scenario);
        scenarioFileStore.save(qapilotDir(serviceId), scenario);
        return scenario;
    }

    public Map<String, Object> get(String serviceId, String scenarioId) {
        return scenarioFileStore.load(qapilotDir(serviceId), scenarioId);
    }

    public Map<String, Object> update(String serviceId, String scenarioId, Map<String, Object> patch) {
        Map<String, Object> current = new LinkedHashMap<>(get(serviceId, scenarioId));
        current.putAll(patch);
        current.put("ts_id", scenarioId);
        scenarioFileStore.save(qapilotDir(serviceId), current);
        return current;
    }

    public void delete(String serviceId, String scenarioId) {
        scenarioFileStore.delete(qapilotDir(serviceId), scenarioId);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> testCases(String serviceId, String scenarioId) {
        Object value = get(serviceId, scenarioId).get("test_cases");
        if (value instanceof List<?> list) {
            return new ArrayList<>((List<Map<String, Object>>) list);
        }
        return List.of();
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
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
