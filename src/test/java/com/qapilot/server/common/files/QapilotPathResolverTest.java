package com.qapilot.server.common.files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qapilot.server.common.config.QapilotProperties;
import com.qapilot.server.common.error.QapilotException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * QapilotPathResolver 단위 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
class QapilotPathResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void qapilotDirResolvesUnderTargetRoot() {
        QapilotPathResolver resolver = new QapilotPathResolver(properties(tempDir));

        Path result = resolver.qapilotDir(tempDir);

        assertThat(result).isEqualTo(tempDir.toAbsolutePath().normalize().resolve(".qapilot"));
    }

    @Test
    void requireExistingQapilotDirReturnsExistingDirectory() throws Exception {
        Files.createDirectories(tempDir.resolve(".qapilot"));
        QapilotPathResolver resolver = new QapilotPathResolver(properties(tempDir));

        Path result = resolver.requireExistingQapilotDir(tempDir);

        assertThat(result).isDirectory();
    }

    @Test
    void requireExistingQapilotDirThrowsWhenMissing() {
        QapilotPathResolver resolver = new QapilotPathResolver(properties(tempDir));

        assertThatThrownBy(() -> resolver.requireExistingQapilotDir(tempDir))
                .isInstanceOf(QapilotException.class)
                .hasMessageContaining(".qapilot");
    }

    private QapilotProperties properties(Path targetRoot) {
        return new QapilotProperties(
                new QapilotProperties.Storage(targetRoot.toString()),
                new QapilotProperties.Fastapi("http://localhost:8001", ""),
                null
        );
    }
}
