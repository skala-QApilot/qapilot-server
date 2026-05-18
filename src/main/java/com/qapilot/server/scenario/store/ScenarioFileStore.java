package com.qapilot.server.scenario.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.JsonFileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * qapilotDir 기반 시나리오 파일 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class ScenarioFileStore {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JsonFileStore jsonFileStore;

    public ScenarioFileStore(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<Map<String, Object>> listAll(Path qapilotDir) {
        Path scenariosDir = scenariosDir(qapilotDir);
        if (!Files.isDirectory(scenariosDir)) {
            return List.of();
        }
        try (var stream = Files.list(scenariosDir)) {
            return stream.filter(path -> path.toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::getFileName))
                    .map(path -> jsonFileStore.read(path, MAP_TYPE))
                    .toList();
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "시나리오 목록을 읽을 수 없습니다.");
        }
    }

    public Map<String, Object> load(Path qapilotDir, String scenarioId) {
        Path path = scenarioPath(qapilotDir, scenarioId);
        if (!Files.exists(path)) {
            throw new QapilotException(ErrorCode.SCENARIO_001);
        }
        return jsonFileStore.read(path, MAP_TYPE);
    }

    public Path save(Path qapilotDir, Map<String, Object> scenario) {
        String scenarioId = stringValue(scenario.get("ts_id"));
        if (scenarioId.isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "ts_id 필드가 필요합니다.");
        }
        Path path = scenarioPath(qapilotDir, scenarioId);
        jsonFileStore.write(path, scenario);
        return path;
    }

    public void delete(Path qapilotDir, String scenarioId) {
        Path path = scenarioPath(qapilotDir, scenarioId);
        if (!Files.exists(path)) {
            throw new QapilotException(ErrorCode.SCENARIO_001);
        }
        try {
            Files.delete(path);
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "시나리오를 삭제할 수 없습니다.");
        }
    }

    public Path scenariosDir(Path qapilotDir) {
        return qapilotDir.resolve("scenarios");
    }

    private Path scenarioPath(Path qapilotDir, String scenarioId) {
        return scenariosDir(qapilotDir).resolve(scenarioId + ".json");
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
