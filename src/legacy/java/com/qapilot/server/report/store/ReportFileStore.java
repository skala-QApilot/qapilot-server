package com.qapilot.server.report.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.run.RunReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * .qapilot/reports 리포트 조회 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class ReportFileStore {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JsonFileStore jsonFileStore;
    private final RunReader runReader;       // PR-15d — fallback 으로 DB run 행에서 리포트 derive

    public ReportFileStore(JsonFileStore jsonFileStore, RunReader runReader) {
        this.jsonFileStore = jsonFileStore;
        this.runReader = runReader;
    }

    public List<Map<String, Object>> list(Path qapilotDir) {
        Path reportsDir = reportsDir(qapilotDir);
        if (!Files.isDirectory(reportsDir)) {
            return List.of();
        }
        try (var stream = Files.list(reportsDir)) {
            return stream.filter(path -> path.toString().endsWith(".json"))
                    .map(path -> jsonFileStore.read(path, MAP_TYPE))
                    .sorted(Comparator.comparing(this::generatedAt).reversed())
                    .toList();
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.FILE_001, "리포트 목록을 읽을 수 없습니다.");
        }
    }

    public Map<String, Object> load(Path qapilotDir, String traceId) {
        Path reportPath = reportsDir(qapilotDir).resolve(traceId + ".json");
        if (Files.exists(reportPath)) {
            return jsonFileStore.read(reportPath, MAP_TYPE);
        }
        return runReader.findById(traceId)
                .map(this::buildFromTrace)
                .orElseThrow(() -> new QapilotException(ErrorCode.REPORT_001));
    }

    private Map<String, Object> buildFromTrace(Map<String, Object> trace) {
        String traceId = String.valueOf(trace.get("trace_id"));
        Object generatedAt = trace.get("completed_at") == null ? Instant.now().toString() : trace.get("completed_at");
        Object summary = trace.get("result_summary") == null ? Map.of() : trace.get("result_summary");
        return Map.of(
                "trace_id", traceId,
                "title", "테스트 리포트 - " + traceId.substring(0, Math.min(8, traceId.length())),
                "generated_at", generatedAt,
                "summary", summary,
                "sections", List.of()
        );
    }

    private String generatedAt(Map<String, Object> report) {
        Object value = report.get("generated_at");
        return value == null ? "" : String.valueOf(value);
    }

    private Path reportsDir(Path qapilotDir) {
        return qapilotDir.resolve("reports");
    }
}
