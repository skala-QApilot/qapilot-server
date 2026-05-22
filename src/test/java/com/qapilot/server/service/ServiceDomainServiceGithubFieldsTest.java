package com.qapilot.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.config.QapilotProperties;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.common.files.QapilotPathResolver;
import com.qapilot.server.service.domain.QapilotService;
import com.qapilot.server.service.dto.ServiceCreateRequest;
import com.qapilot.server.service.store.ServiceFileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ServiceDomainService.create() 의 GitHub 필드 보관 검증.
 *
 * <p>Author: C
 * <br>Created: 2026-05-22
 */
class ServiceDomainServiceGithubFieldsTest {

    private ServiceDomainService newService(Path targetRoot) {
        QapilotProperties properties = new QapilotProperties(
                new QapilotProperties.Storage(targetRoot.toString()),
                new QapilotProperties.Fastapi("http://localhost:8001", "test-token"),
                new QapilotProperties.Cors(null, null, null, null)
        );
        QapilotPathResolver pathResolver = new QapilotPathResolver(properties);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonFileStore jsonFileStore = new JsonFileStore(objectMapper);
        ServiceFileStore fileStore = new ServiceFileStore(jsonFileStore, pathResolver);
        return new ServiceDomainService(fileStore, new ServiceSlugGenerator(), pathResolver, properties);
    }

    @Test
    void createPersistsGithubFields(@TempDir Path tempDir) throws Exception {
        Path targetRoot = tempDir.resolve("sut");
        Files.createDirectories(targetRoot);
        ServiceDomainService domain = newService(targetRoot);

        ServiceCreateRequest request = new ServiceCreateRequest(
                "My Service", "desc",
                targetRoot.toString(),
                "https://github.com/owner/repo",
                "ghp_xxx",
                "main"
        );
        QapilotService created = domain.create(request);

        assertThat(created.repoUrl()).isEqualTo("https://github.com/owner/repo");
        assertThat(created.repoToken()).isEqualTo("ghp_xxx");
        assertThat(created.repoBranch()).isEqualTo("main");
        // 다른 메타도 정상
        assertThat(created.displayName()).isEqualTo("My Service");
        assertThat(created.projectSlug()).isEqualTo("my-service");
    }

    @Test
    void createAcceptsNullGithubFields(@TempDir Path tempDir) throws Exception {
        Path targetRoot = tempDir.resolve("sut");
        Files.createDirectories(targetRoot);
        ServiceDomainService domain = newService(targetRoot);

        ServiceCreateRequest request = new ServiceCreateRequest(
                "Solo Service", null,
                targetRoot.toString(),
                null, null, null
        );
        QapilotService created = domain.create(request);

        assertThat(created.repoUrl()).isNull();
        assertThat(created.repoToken()).isNull();
        assertThat(created.repoBranch()).isNull();
    }

    @Test
    void createTrimsAndNullifiesBlankGithubFields(@TempDir Path tempDir) throws Exception {
        Path targetRoot = tempDir.resolve("sut");
        Files.createDirectories(targetRoot);
        ServiceDomainService domain = newService(targetRoot);

        ServiceCreateRequest request = new ServiceCreateRequest(
                "Blank Fields Service", null,
                targetRoot.toString(),
                "  https://github.com/owner/repo  ",
                "",
                "   "
        );
        QapilotService created = domain.create(request);

        assertThat(created.repoUrl()).isEqualTo("https://github.com/owner/repo");
        assertThat(created.repoToken()).isNull();
        assertThat(created.repoBranch()).isNull();
    }

    @Test
    void rotateTokenPreservesGithubFields(@TempDir Path tempDir) throws Exception {
        Path targetRoot = tempDir.resolve("sut");
        Files.createDirectories(targetRoot);
        ServiceDomainService domain = newService(targetRoot);

        QapilotService created = domain.create(new ServiceCreateRequest(
                "Rotated", null, targetRoot.toString(),
                "https://github.com/x/y", "tok", "dev"
        ));
        QapilotService rotated = domain.rotateToken(created.serviceId());

        assertThat(rotated.serverAuthToken()).isNotEqualTo(created.serverAuthToken());
        assertThat(rotated.repoUrl()).isEqualTo("https://github.com/x/y");
        assertThat(rotated.repoToken()).isEqualTo("tok");
        assertThat(rotated.repoBranch()).isEqualTo("dev");
    }
}
