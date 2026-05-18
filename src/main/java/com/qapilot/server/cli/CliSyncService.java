package com.qapilot.server.cli;

import com.qapilot.server.cli.dto.CliSyncItemError;
import com.qapilot.server.cli.dto.CliSyncRequest;
import com.qapilot.server.cli.dto.CliSyncResult;
import com.qapilot.server.cli.dto.GeneratedCodeSyncItem;
import com.qapilot.server.cli.dto.ResultSyncItem;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * CLI 산출물 ingestion 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class CliSyncService {

    private final JsonFileStore jsonFileStore;

    public CliSyncService(JsonFileStore jsonFileStore) {
        this.jsonFileStore = jsonFileStore;
    }

    public CliSyncResult syncScenarios(QapilotService service, CliSyncRequest<Map<String, Object>> request) {
        validateItems(request);
        Path dir = qapilotDir(service).resolve("scenarios");
        return syncItems(request.items(), (item, index) -> {
            String tsId = required(item.get("ts_id"), "ts_id 필드가 필요합니다.");
            Path path = dir.resolve(tsId + ".json");
            jsonFileStore.write(path, item);
            return path.toString();
        });
    }

    public CliSyncResult syncGeneratedCode(QapilotService service, CliSyncRequest<GeneratedCodeSyncItem> request) {
        validateItems(request);
        Path dir = qapilotDir(service).resolve("generated-code");
        return syncItems(request.items(), (item, index) -> {
            String tcId = required(item.tcId(), "tc_id 필드가 필요합니다.");
            String code = required(item.code(), "code 필드가 필요합니다.");
            Path path = dir.resolve(tcId + ".js");
            Files.createDirectories(path.getParent());
            Files.writeString(path, code);
            return path.toString();
        });
    }

    public CliSyncResult syncResults(QapilotService service, CliSyncRequest<ResultSyncItem> request) {
        validateItems(request);
        Path dir = qapilotDir(service).resolve("results");
        return syncItems(request.items(), (item, index) -> {
            String traceId = required(item.traceId(), "trace_id 필드가 필요합니다.");
            Path path = dir.resolve(traceId + ".json");
            jsonFileStore.write(path, item.data() == null ? Map.of() : item.data());
            return path.toString();
        });
    }

    private <T> CliSyncResult syncItems(List<T> items, SyncWriter<T> writer) {
        List<String> paths = new ArrayList<>();
        List<CliSyncItemError> errors = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            try {
                paths.add(writer.write(items.get(index), index));
            } catch (Exception e) {
                errors.add(new CliSyncItemError(index, e.getMessage()));
            }
        }
        return new CliSyncResult(paths.size(), errors.size(), paths, errors);
    }

    private void validateItems(CliSyncRequest<?> request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new QapilotException(ErrorCode.CLI_SYNC_002);
        }
    }

    private Path qapilotDir(QapilotService service) {
        try {
            return Path.of(service.qapilotDir());
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.CLI_SYNC_003);
        }
    }

    private String required(Object value, String message) {
        String result = value == null ? "" : String.valueOf(value).trim();
        if (result.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    @FunctionalInterface
    private interface SyncWriter<T> {
        String write(T item, int index) throws Exception;
    }
}
