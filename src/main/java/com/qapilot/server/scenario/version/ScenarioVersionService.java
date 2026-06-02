package com.qapilot.server.scenario.version;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.scenario.ScenarioReader;
import com.qapilot.server.scenario.version.domain.ScenarioVersion;
import com.qapilot.server.scenario.version.dto.CreateScenarioVersionRequest;
import com.qapilot.server.scenario.version.dto.UpdateScenarioVersionRequest;
import com.qapilot.server.scenario.version.persistence.ScenarioSnapshotEntity;
import com.qapilot.server.scenario.version.persistence.ScenarioSnapshotRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 시나리오 스냅샷 (file 측 ScenarioVersion = 사용자 명시 저장) 관리. PR-16 — JPA only.
 *
 * <p>DB scenario_versions (자동 변경 이력) 와는 다른 개념 — scenario_snapshots 테이블 사용.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18, rewritten 2026-06-02
 */
@Service
public class ScenarioVersionService {

    private static final TypeReference<List<Map<String, Object>>> SNAPSHOT_TYPE = new TypeReference<>() {};

    private final ScenarioReader scenarioReader;
    private final ScenarioSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    public ScenarioVersionService(
            ScenarioReader scenarioReader,
            ScenarioSnapshotRepository snapshotRepository,
            ObjectMapper objectMapper
    ) {
        this.scenarioReader = scenarioReader;
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
    }

    public List<ScenarioVersion> list(String serviceId) {
        return snapshotRepository.findAllByServiceIdOrderByCreatedAtDesc(UUID.fromString(serviceId)).stream()
                .map(this::toDomain)
                .toList();
    }

    public ScenarioVersion create(String serviceId, CreateScenarioVersionRequest request) {
        if (request.label() == null || request.label().isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "label 필드가 필요합니다.");
        }
        List<Map<String, Object>> snapshot = scenarioReader.listByServiceId(serviceId);
        ScenarioSnapshotEntity entity = new ScenarioSnapshotEntity();
        entity.setId(UUID.randomUUID());
        entity.setServiceId(UUID.fromString(serviceId));
        entity.setLabel(request.label());
        entity.setDescription(request.description() == null ? "" : request.description());
        entity.setScenariosPayload(toJson(snapshot));
        entity.setFavorite(false);
        snapshotRepository.save(entity);
        return toDomain(entity);
    }

    public ScenarioVersion update(String serviceId, String versionId, UpdateScenarioVersionRequest request) {
        ScenarioSnapshotEntity entity = requireEntity(serviceId, versionId);
        if (request.label() != null) {
            entity.setLabel(request.label());
        }
        if (request.isFavorite() != null) {
            entity.setFavorite(request.isFavorite());
        }
        snapshotRepository.save(entity);
        return toDomain(entity);
    }

    public void delete(String serviceId, String versionId) {
        ScenarioSnapshotEntity entity = requireEntity(serviceId, versionId);
        snapshotRepository.delete(entity);
    }

    public Map<String, Object> diff(String serviceId, String versionId) {
        ScenarioSnapshotEntity current = requireEntity(serviceId, versionId);
        List<ScenarioSnapshotEntity> all = snapshotRepository
                .findAllByServiceIdOrderByCreatedAtDesc(UUID.fromString(serviceId));
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(current.getId())) { idx = i; break; }
        }
        ScenarioSnapshotEntity previous = (idx >= 0 && idx + 1 < all.size()) ? all.get(idx + 1) : null;

        Set<String> currentIds = tsIds(fromJson(current.getScenariosPayload()));
        Set<String> previousIds = previous != null ? tsIds(fromJson(previous.getScenariosPayload())) : Set.of();

        List<String> added = currentIds.stream().filter(id -> !previousIds.contains(id)).sorted().toList();
        List<String> removed = previousIds.stream().filter(id -> !currentIds.contains(id)).sorted().toList();

        Map<String, Map<String, Object>> currentMap = scenarioMap(fromJson(current.getScenariosPayload()));
        Map<String, Map<String, Object>> previousMap = previous != null
                ? scenarioMap(fromJson(previous.getScenariosPayload())) : Map.of();
        List<String> modified = currentIds.stream()
                .filter(previousIds::contains)
                .filter(id -> !currentMap.get(id).equals(previousMap.get(id)))
                .sorted()
                .toList();

        return Map.of("added", added, "removed", removed, "modified", modified);
    }

    private ScenarioSnapshotEntity requireEntity(String serviceId, String versionId) {
        try {
            Optional<ScenarioSnapshotEntity> opt = snapshotRepository.findById(UUID.fromString(versionId));
            if (opt.isEmpty() || !opt.get().getServiceId().equals(UUID.fromString(serviceId))) {
                throw new QapilotException(ErrorCode.SCENARIO_001);
            }
            return opt.get();
        } catch (IllegalArgumentException e) {
            throw new QapilotException(ErrorCode.SCENARIO_001);
        }
    }

    private ScenarioVersion toDomain(ScenarioSnapshotEntity e) {
        return new ScenarioVersion(
                e.getId().toString(),
                e.getServiceId().toString(),
                e.getLabel(),
                e.getDescription(),
                fromJson(e.getScenariosPayload()),
                e.isFavorite(),
                e.getCreatedAt() == null ? null : e.getCreatedAt().toString()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<Map<String, Object>> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, SNAPSHOT_TYPE);
        } catch (Exception ex) {
            return List.of();
        }
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
}
