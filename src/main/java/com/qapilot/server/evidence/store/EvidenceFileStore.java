package com.qapilot.server.evidence.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.JsonFileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * .qapilot/evidence read-only 조회 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class EvidenceFileStore {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JsonFileStore jsonFileStore;

    public EvidenceFileStore(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public Map<String, Object> index(Path qapilotDir, String traceId) {
        Path path = evidenceDir(qapilotDir, traceId).resolve("index.json");
        if (!Files.exists(path)) {
            throw new QapilotException(ErrorCode.EVIDENCE_001);
        }
        return jsonFileStore.read(path, MAP_TYPE);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> tcEvidence(Path qapilotDir, String traceId, String tcId) {
        Map<String, Object> index = index(qapilotDir, traceId);
        Object evidences = index.get("tc_evidences");
        if (!(evidences instanceof List<?> list)) {
            throw new QapilotException(ErrorCode.EVIDENCE_001);
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> evidence && tcId.equals(evidence.get("tc_id"))) {
                return enrichFilePaths(qapilotDir, traceId, tcId, (Map<String, Object>) evidence);
            }
        }
        throw new QapilotException(ErrorCode.EVIDENCE_001);
    }

    private Map<String, Object> enrichFilePaths(
            Path qapilotDir,
            String traceId,
            String tcId,
            Map<String, Object> evidence
    ) {
        Object filesValue = evidence.get("files");
        List<String> files = filesValue instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        Path tcDir = evidenceDir(qapilotDir, traceId).resolve(tcId);
        List<String> filePaths = files.stream()
                .map(tcDir::resolve)
                .filter(Files::exists)
                .map(Path::toString)
                .toList();
        return Map.of(
                "tc_id", tcId,
                "files", files,
                "file_paths", filePaths,
                "created_at", evidence.getOrDefault("created_at", "")
        );
    }

    private Path evidenceDir(Path qapilotDir, String traceId) {
        return qapilotDir.resolve("evidence").resolve(traceId);
    }
}
