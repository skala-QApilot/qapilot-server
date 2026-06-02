package com.qapilot.server.scenario.version;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.scenario.ScenarioReader;
import com.qapilot.server.scenario.version.domain.ScenarioVersion;
import com.qapilot.server.scenario.version.dto.CreateScenarioVersionRequest;
import com.qapilot.server.scenario.version.dto.UpdateScenarioVersionRequest;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 시나리오 버전 관리 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class ScenarioVersionService {

    private final ServiceDomainService serviceDomainService;
    private final ScenarioVersionFileStore versionFileStore;
    private final ScenarioReader scenarioReader;

    public ScenarioVersionService(
            ServiceDomainService serviceDomainService,
            ScenarioVersionFileStore versionFileStore,
            ScenarioReader scenarioReader
    ) {
        this.serviceDomainService = serviceDomainService;
        this.versionFileStore = versionFileStore;
        this.scenarioReader = scenarioReader;
    }

    public List<ScenarioVersion> list(String serviceId) {
        return versionFileStore.listAll(qapilotDir(serviceId));
    }

    public ScenarioVersion create(String serviceId, CreateScenarioVersionRequest request) {
        if (request.label() == null || request.label().isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "label 필드가 필요합니다.");
        }
        Path qapilotDir = qapilotDir(serviceId);
        List<Map<String, Object>> snapshot = scenarioReader.listByServiceId(serviceId);
        ScenarioVersion version = new ScenarioVersion(
                UUID.randomUUID().toString(),
                serviceId,
                request.label(),
                request.description() == null ? "" : request.description(),
                snapshot,
                false,
                Instant.now().toString()
        );
        versionFileStore.save(qapilotDir, version);
        return version;
    }

    public ScenarioVersion update(String serviceId, String versionId, UpdateScenarioVersionRequest request) {
        Path qapilotDir = qapilotDir(serviceId);
        ScenarioVersion current = versionFileStore.load(qapilotDir, versionId);
        ScenarioVersion updated = new ScenarioVersion(
                current.versionId(),
                current.serviceId(),
                request.label() != null ? request.label() : current.label(),
                current.description(),
                current.scenariosSnapshot(),
                request.isFavorite() != null ? request.isFavorite() : current.isFavorite(),
                current.createdAt()
        );
        versionFileStore.save(qapilotDir, updated);
        return updated;
    }

    public void delete(String serviceId, String versionId) {
        versionFileStore.delete(qapilotDir(serviceId), versionId);
    }

    public Map<String, Object> diff(String serviceId, String versionId) {
        Path qapilotDir = qapilotDir(serviceId);
        ScenarioVersion current = versionFileStore.load(qapilotDir, versionId);
        List<ScenarioVersion> all = versionFileStore.listAll(qapilotDir);

        // createdAt asc 정렬 후 직전 버전 탐색
        List<ScenarioVersion> sorted = all.stream()
                .sorted((a, b) -> a.createdAt().compareTo(b.createdAt()))
                .toList();
        int idx = sorted.stream().map(ScenarioVersion::versionId).toList().indexOf(versionId);
        ScenarioVersion previous = (idx > 0) ? sorted.get(idx - 1) : null;

        Set<String> currentIds = tsIds(current.scenariosSnapshot());
        Set<String> previousIds = previous != null ? tsIds(previous.scenariosSnapshot()) : Set.of();

        List<String> added = currentIds.stream().filter(id -> !previousIds.contains(id)).sorted().toList();
        List<String> removed = previousIds.stream().filter(id -> !currentIds.contains(id)).sorted().toList();

        Map<String, Map<String, Object>> currentMap = scenarioMap(current.scenariosSnapshot());
        Map<String, Map<String, Object>> previousMap = previous != null ? scenarioMap(previous.scenariosSnapshot()) : Map.of();
        List<String> modified = currentIds.stream()
                .filter(previousIds::contains)
                .filter(id -> !currentMap.get(id).equals(previousMap.get(id)))
                .sorted()
                .toList();

        return Map.of("added", added, "removed", removed, "modified", modified);
    }

    private Set<String> tsIds(List<Map<String, Object>> scenarios) {
        if (scenarios == null) return Set.of();
        return scenarios.stream()
                .map(s -> String.valueOf(s.getOrDefault("ts_id", "")))
                .filter(id -> !id.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Map<String, Map<String, Object>> scenarioMap(List<Map<String, Object>> scenarios) {
        if (scenarios == null) return Map.of();
        return scenarios.stream()
                .collect(Collectors.toMap(
                        s -> String.valueOf(s.getOrDefault("ts_id", "")),
                        s -> s,
                        (a, b) -> a
                ));
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
    }
}
