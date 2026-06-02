package com.qapilot.server.rtm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.rtm.domain.RtmVersion;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RTM 버전 파일 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class RtmFileStore {

    private static final TypeReference<RtmVersion> TYPE = new TypeReference<>() {
    };

    private final JsonFileStore jsonFileStore;

    public RtmFileStore(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public List<RtmVersion> listAll(Path qapilotDir) {
        Path dir = rtmDir(qapilotDir);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> jsonFileStore.read(p, TYPE))
                    .sorted(Comparator.comparing(RtmVersion::createdAt).reversed())
                    .toList();
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "RTM 목록을 읽을 수 없습니다.");
        }
    }

    public RtmVersion load(Path qapilotDir, String rtmVersionId) {
        Path path = rtmPath(qapilotDir, rtmVersionId);
        if (!Files.exists(path)) {
            throw new QapilotException(ErrorCode.RTM_001);
        }
        return jsonFileStore.read(path, TYPE);
    }

    public void save(Path qapilotDir, RtmVersion version) {
        jsonFileStore.write(rtmPath(qapilotDir, version.rtmVersionId()), version);
    }

    private Path rtmDir(Path qapilotDir) {
        return qapilotDir.resolve("rtm-versions");
    }

    private Path rtmPath(Path qapilotDir, String rtmVersionId) {
        return rtmDir(qapilotDir).resolve(rtmVersionId + ".json");
    }
}
