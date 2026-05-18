package com.qapilot.server.trace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.JsonFileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * qapilotDir 기반 trace 파일 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class TraceFileStore {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JsonFileStore jsonFileStore;

    public TraceFileStore(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public Optional<Map<String, Object>> findById(Path qapilotDir, String traceId) {
        Path path = tracePath(qapilotDir, traceId);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(jsonFileStore.read(path, MAP_TYPE));
    }

    public Map<String, Object> load(Path qapilotDir, String traceId) {
        return findById(qapilotDir, traceId).orElseThrow(() -> new QapilotException(ErrorCode.RUN_001));
    }

    public List<Map<String, Object>> listAll(Path qapilotDir) {
        Path tracesDir = tracesDir(qapilotDir);
        if (!Files.isDirectory(tracesDir)) {
            return List.of();
        }
        try (var stream = Files.list(tracesDir)) {
            return stream.filter(path -> path.toString().endsWith(".json"))
                    .map(path -> jsonFileStore.read(path, MAP_TYPE))
                    .sorted(Comparator.comparing(this::startedAt).reversed())
                    .toList();
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "trace 목록을 읽을 수 없습니다.");
        }
    }

    public Path tracePath(Path qapilotDir, String traceId) {
        return tracesDir(qapilotDir).resolve(traceId + ".json");
    }

    private Path tracesDir(Path qapilotDir) {
        return qapilotDir.resolve("traces");
    }

    private String startedAt(Map<String, Object> trace) {
        Object value = trace.get("started_at");
        return value == null ? "" : String.valueOf(value);
    }
}
