package com.qapilot.server.graph;

import com.qapilot.server.scenario.store.ScenarioFileStore;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 시나리오 그래프/플로우 동적 생성 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class GraphService {

    private final ServiceDomainService serviceDomainService;
    private final ScenarioFileStore scenarioFileStore;

    public GraphService(ServiceDomainService serviceDomainService, ScenarioFileStore scenarioFileStore) {
        this.serviceDomainService = serviceDomainService;
        this.scenarioFileStore = scenarioFileStore;
    }

    public Map<String, Object> graph(String serviceId) {
        List<Map<String, Object>> scenarios = scenarioFileStore.listAll(qapilotDir(serviceId));

        List<Map<String, Object>> nodes = scenarios.stream().map(s -> {
            List<?> testCases = asList(s.get("test_cases"));
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("id", s.get("ts_id"));
            node.put("name", s.get("name"));
            node.put("tcCount", testCases.size());
            return node;
        }).toList();

        // 동일 affected_file을 공유하는 시나리오 쌍에 edge 생성
        List<Map<String, Object>> edges = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < scenarios.size(); i++) {
            for (int j = i + 1; j < scenarios.size(); j++) {
                Set<String> filesI = fileSet(scenarios.get(i));
                Set<String> filesJ = fileSet(scenarios.get(j));
                filesI.retainAll(filesJ);
                if (!filesI.isEmpty()) {
                    String srcId = String.valueOf(scenarios.get(i).get("ts_id"));
                    String tgtId = String.valueOf(scenarios.get(j).get("ts_id"));
                    String key = srcId + "→" + tgtId;
                    if (seen.add(key)) {
                        edges.add(Map.of("source", srcId, "target", tgtId));
                    }
                }
            }
        }

        return Map.of("graph", Map.of("nodes", nodes, "edges", edges, "generatedAt", Instant.now().toString()));
    }

    public Map<String, Object> flow(String serviceId) {
        List<Map<String, Object>> scenarios = scenarioFileStore.listAll(qapilotDir(serviceId));

        List<Map<String, Object>> flows = scenarios.stream().map(s -> {
            List<Map<String, Object>> steps = asList(s.get("test_cases")).stream()
                    .filter(tc -> tc instanceof Map)
                    .map(tc -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> tcMap = (Map<String, Object>) tc;
                        Map<String, Object> step = new LinkedHashMap<>();
                        step.put("tcId", tcMap.get("tc_id"));
                        step.put("name", tcMap.get("name"));
                        step.put("given", tcMap.get("given"));
                        step.put("when", tcMap.get("when"));
                        step.put("then", tcMap.get("then"));
                        return step;
                    })
                    .toList();
            Map<String, Object> flow = new LinkedHashMap<>();
            flow.put("tsId", s.get("ts_id"));
            flow.put("tsName", s.get("name"));
            flow.put("steps", steps);
            return flow;
        }).toList();

        return Map.of("flow", Map.of("flows", flows, "generatedAt", Instant.now().toString()));
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> asList(Object value) {
        if (value instanceof List) return (List<T>) value;
        return List.of();
    }

    private Set<String> fileSet(Map<String, Object> scenario) {
        Set<String> result = new HashSet<>();
        List<?> files = asList(scenario.get("affected_files"));
        for (Object f : files) {
            if (f != null) result.add(String.valueOf(f));
        }
        return result;
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
    }
}
