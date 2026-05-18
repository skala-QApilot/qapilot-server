package com.qapilot.server.retest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.retest.domain.RetestGroup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 재테스트 그룹 파일 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class RetestFileStore {

    private static final TypeReference<RetestGroup> TYPE = new TypeReference<>() {
    };

    private final JsonFileStore jsonFileStore;

    public RetestFileStore(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<RetestGroup> listAll(Path qapilotDir) {
        Path dir = retestDir(qapilotDir);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> jsonFileStore.read(p, TYPE))
                    .sorted(Comparator.comparing(RetestGroup::createdAt).reversed())
                    .toList();
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "재테스트 그룹 목록을 읽을 수 없습니다.");
        }
    }

    public void save(Path qapilotDir, RetestGroup group) {
        jsonFileStore.write(retestPath(qapilotDir, group.retestGroupId()), group);
    }

    private Path retestDir(Path qapilotDir) {
        return qapilotDir.resolve("retest-groups");
    }

    private Path retestPath(Path qapilotDir, String retestGroupId) {
        return retestDir(qapilotDir).resolve(retestGroupId + ".json");
    }
}
