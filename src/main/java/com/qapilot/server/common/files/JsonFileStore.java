package com.qapilot.server.common.files;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * JSON 파일 read/write 공통 유틸.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class JsonFileStore {

    private final ObjectMapper objectMapper;

    public JsonFileStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> T read(Path path, Class<T> type) {
        try {
            return objectMapper.readValue(path.toFile(), type);
        } catch (IOException e) {
            throw new QapilotException(ErrorCode.FILE_001, "JSON 파일을 읽을 수 없습니다: " + path);
        }
    }

    public <T> T read(Path path, TypeReference<T> type) {
        try {
            return objectMapper.readValue(path.toFile(), type);
        } catch (IOException e) {
            throw new QapilotException(ErrorCode.FILE_001, "JSON 파일을 읽을 수 없습니다: " + path);
        }
    }

    public void write(Path path, Object data) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), data);
        } catch (IOException e) {
            throw new QapilotException(ErrorCode.FILE_001, "JSON 파일을 저장할 수 없습니다: " + path);
        }
    }
}
