package com.qapilot.server.scenario.group;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.scenario.group.domain.ScenarioGroup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 시나리오 그룹 파일 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class ScenarioGroupFileStore {

    private static final TypeReference<ScenarioGroup> TYPE = new TypeReference<>() {
    };

    private final JsonFileStore jsonFileStore;

    public ScenarioGroupFileStore(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<ScenarioGroup> listAll(Path qapilotDir) {
        Path dir = groupsDir(qapilotDir);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> jsonFileStore.read(p, TYPE))
                    .sorted(Comparator.comparing(ScenarioGroup::createdAt).reversed())
                    .toList();
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "시나리오 그룹 목록을 읽을 수 없습니다.");
        }
    }

    public ScenarioGroup load(Path qapilotDir, String groupId) {
        Path path = groupPath(qapilotDir, groupId);
        if (!Files.exists(path)) {
            throw new QapilotException(ErrorCode.GROUP_001);
        }
        return jsonFileStore.read(path, TYPE);
    }

    public void save(Path qapilotDir, ScenarioGroup group) {
        jsonFileStore.write(groupPath(qapilotDir, group.groupId()), group);
    }

    public void delete(Path qapilotDir, String groupId) {
        Path path = groupPath(qapilotDir, groupId);
        if (!Files.exists(path)) {
            throw new QapilotException(ErrorCode.GROUP_001);
        }
        try {
            Files.delete(path);
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "시나리오 그룹을 삭제할 수 없습니다.");
        }
    }

    private Path groupsDir(Path qapilotDir) {
        return qapilotDir.resolve("scenario-groups");
    }

    private Path groupPath(Path qapilotDir, String groupId) {
        return groupsDir(qapilotDir).resolve(groupId + ".json");
    }
}
