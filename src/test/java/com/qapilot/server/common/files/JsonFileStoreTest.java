package com.qapilot.server.common.files;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * JsonFileStore 단위 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
class JsonFileStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void writeAndReadObject() {
        JsonFileStore store = new JsonFileStore(new ObjectMapper());
        Path path = tempDir.resolve("meta/project.json");

        store.write(path, Map.of("name", "system-under-test"));
        Map<String, String> result = store.read(path, new TypeReference<>() {
        });

        assertThat(result).containsEntry("name", "system-under-test");
    }

    @Test
    void writeAndReadList() {
        JsonFileStore store = new JsonFileStore(new ObjectMapper());
        Path path = tempDir.resolve("services.json");

        store.write(path, List.of(Map.of("project_slug", "sut")));
        List<Map<String, String>> result = store.read(path, new TypeReference<>() {
        });

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("project_slug", "sut");
    }
}
