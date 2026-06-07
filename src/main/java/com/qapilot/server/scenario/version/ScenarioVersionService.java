package com.qapilot.server.scenario.version;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.scenario.ScenarioReader;
import com.qapilot.server.scenario.ScenarioWriter;
import com.qapilot.server.scenario.version.domain.ScenarioVersion;
import com.qapilot.server.scenario.version.dto.CreateScenarioVersionRequest;
import com.qapilot.server.scenario.version.dto.UpdateScenarioVersionRequest;
import com.qapilot.server.scenario.version.persistence.ScenarioVersionEntity;
import com.qapilot.server.scenario.version.persistence.ScenarioVersionRepository;
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
    private final ScenarioWriter scenarioWriter;
    private final ScenarioVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    public ScenarioVersionService(
            ScenarioReader scenarioReader,
            ScenarioWriter scenarioWriter,
            ScenarioVersionRepository versionRepository,
            ObjectMapper objectMapper
    ) {
        this.scenarioReader = scenarioReader;
        this.scenarioWriter = scenarioWriter;
        this.versionRepository = versionRepository;
        this.objectMapper = objectMapper;
    }

    public List<ScenarioVersion> list(String serviceId) {
        return versionRepository.findAllByServiceIdOrderByCreatedAtDesc(UUID.fromString(serviceId)).stream()
                .map(this::toDomain)
                .toList();
    }

    public ScenarioVersion create(String serviceId, CreateScenarioVersionRequest request) {
        if (request.label() == null || request.label().isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "label 필드가 필요합니다.");
        }
        List<Map<String, Object>> snapshot = scenarioReader.listByServiceId(serviceId);
        ScenarioVersionEntity entity = new ScenarioVersionEntity();
        entity.setId(UUID.randomUUID());
        entity.setServiceId(UUID.fromString(serviceId));
        entity.setLabel(request.label());
        entity.setDescription(request.description() == null ? "" : request.description());
        entity.setScenariosPayload(toJson(snapshot));
        entity.setFavorite(false);
        versionRepository.save(entity);
        return toDomain(entity);
    }

    public ScenarioVersion update(String serviceId, String versionId, UpdateScenarioVersionRequest request) {
        ScenarioVersionEntity entity = requireEntity(serviceId, versionId);
        if (request.label() != null) {
            entity.setLabel(request.label());
        }
        if (request.isFavorite() != null) {
            entity.setFavorite(request.isFavorite());
        }
        versionRepository.save(entity);
        return toDomain(entity);
    }

    public void delete(String serviceId, String versionId) {
        ScenarioVersionEntity entity = requireEntity(serviceId, versionId);
        versionRepository.delete(entity);
    }

    /**
     * 박힌 마일스톤의 시나리오들을 현재 작업 상태로 복원.
     * 각 ts_id 별로 scenarios 에 새 version_number = MAX+1 row 를 INSERT (이력 보존).
     *
     * @return 복원된 시나리오 개수
     */
    public int restore(String serviceId, String versionId) {
        return restoreWithMeta(serviceId, versionId).get("restoredCount") instanceof Integer n ? n : 0;
    }

    /**
     * 버전 복원 + 스냅샷 생성 시각 반환.
     * snapshotCreatedAt 을 이용해 FastAPI 가 해당 시점의 generated_code 를 복원한다.
     */
    public Map<String, Object> restoreWithMeta(String serviceId, String versionId) {
        ScenarioVersionEntity entity = requireEntity(serviceId, versionId);
        List<Map<String, Object>> scenarios = fromJson(entity.getScenariosPayload());
        int restored = 0;
        for (Map<String, Object> ts : scenarios) {
            Object tsIdObj = ts.get("ts_id");
            if (tsIdObj == null) continue;
            scenarioWriter.upsertVersion(serviceId, String.valueOf(tsIdObj), ts);
            restored += 1;
        }
        String snapshotCreatedAt = entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString();
        return Map.of("restoredCount", restored, "snapshotCreatedAt", snapshotCreatedAt == null ? "" : snapshotCreatedAt);
    }

    public Map<String, Object> diff(String serviceId, String versionId) {
        ScenarioVersionEntity current = requireEntity(serviceId, versionId);
        List<ScenarioVersionEntity> all = versionRepository
                .findAllByServiceIdOrderByCreatedAtDesc(UUID.fromString(serviceId));
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(current.getId())) { idx = i; break; }
        }
        ScenarioVersionEntity previous = (idx >= 0 && idx + 1 < all.size()) ? all.get(idx + 1) : null;

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

    private ScenarioVersionEntity requireEntity(String serviceId, String versionId) {
        try {
            Optional<ScenarioVersionEntity> opt = versionRepository.findById(UUID.fromString(versionId));
            if (opt.isEmpty() || !opt.get().getServiceId().equals(UUID.fromString(serviceId))) {
                throw new QapilotException(ErrorCode.SCENARIO_001);
            }
            return opt.get();
        } catch (IllegalArgumentException e) {
            throw new QapilotException(ErrorCode.SCENARIO_001);
        }
    }

    private ScenarioVersion toDomain(ScenarioVersionEntity e) {
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
