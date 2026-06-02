package com.qapilot.server.scenario.change;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.scenario.change.domain.ChangeRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 변경 요청 파일 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class ChangeRequestFileStore {

    private static final TypeReference<ChangeRequest> TYPE = new TypeReference<>() {
    };

    private final JsonFileStore jsonFileStore;

    public ChangeRequestFileStore(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<ChangeRequest> listAll(Path qapilotDir) {
        Path dir = changeRequestsDir(qapilotDir);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> jsonFileStore.read(p, TYPE))
                    .sorted(Comparator.comparing(ChangeRequest::createdAt).reversed())
                    .toList();
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "변경 요청 목록을 읽을 수 없습니다.");
        }
    }

    public ChangeRequest load(Path qapilotDir, String requestId) {
        Path path = changeRequestPath(qapilotDir, requestId);
        if (!Files.exists(path)) {
            throw new QapilotException(ErrorCode.CHANGE_REQUEST_001);
        }
        return jsonFileStore.read(path, TYPE);
    }

    public void save(Path qapilotDir, ChangeRequest request) {
        jsonFileStore.write(changeRequestPath(qapilotDir, request.requestId()), request);
    }

    private Path changeRequestsDir(Path qapilotDir) {
        return qapilotDir.resolve("change-requests");
    }

    private Path changeRequestPath(Path qapilotDir, String requestId) {
        return changeRequestsDir(qapilotDir).resolve(requestId + ".json");
    }
}
