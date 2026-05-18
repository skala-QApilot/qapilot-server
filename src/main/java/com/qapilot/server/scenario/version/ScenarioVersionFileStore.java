package com.qapilot.server.scenario.version;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.scenario.version.domain.ScenarioVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 시나리오 버전 파일 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class ScenarioVersionFileStore {

    private static final TypeReference<ScenarioVersion> TYPE = new TypeReference<>() {
    };

    private final JsonFileStore jsonFileStore;

    public ScenarioVersionFileStore(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<ScenarioVersion> listAll(Path qapilotDir) {
        Path dir = versionsDir(qapilotDir);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> jsonFileStore.read(p, TYPE))
                    .sorted(Comparator.comparing(ScenarioVersion::createdAt).reversed())
                    .toList();
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "시나리오 버전 목록을 읽을 수 없습니다.");
        }
    }

    public ScenarioVersion load(Path qapilotDir, String versionId) {
        Path path = versionPath(qapilotDir, versionId);
        if (!Files.exists(path)) {
            throw new QapilotException(ErrorCode.VERSION_001);
        }
        return jsonFileStore.read(path, TYPE);
    }

    public Optional<ScenarioVersion> findById(Path qapilotDir, String versionId) {
        Path path = versionPath(qapilotDir, versionId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(jsonFileStore.read(path, TYPE));
    }

    public void save(Path qapilotDir, ScenarioVersion version) {
        jsonFileStore.write(versionPath(qapilotDir, version.versionId()), version);
    }

    public void delete(Path qapilotDir, String versionId) {
        Path path = versionPath(qapilotDir, versionId);
        if (!Files.exists(path)) {
            throw new QapilotException(ErrorCode.VERSION_001);
        }
        try {
            Files.delete(path);
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "시나리오 버전을 삭제할 수 없습니다.");
        }
    }

    private Path versionsDir(Path qapilotDir) {
        return qapilotDir.resolve("scenario-versions");
    }

    private Path versionPath(Path qapilotDir, String versionId) {
        return versionsDir(qapilotDir).resolve(versionId + ".json");
    }
}
