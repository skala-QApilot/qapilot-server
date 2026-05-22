package com.qapilot.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.config.QapilotProperties;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.common.files.QapilotPathResolver;
import com.qapilot.server.service.domain.QapilotService;
import com.qapilot.server.service.domain.RepoConfig;
import com.qapilot.server.service.dto.ServiceCreateRequest;
import com.qapilot.server.service.store.ServiceFileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ServiceDomainService.create() 의 멀티 레포 + stagingUrl 보관 검증.
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
    void createPersistsMultipleReposAndStagingUrl(@TempDir Path tempDir) throws Exception {
        Path targetRoot = tempDir.resolve("sut");
        Files.createDirectories(targetRoot);
        ServiceDomainService domain = newService(targetRoot);

        ServiceCreateRequest request = new ServiceCreateRequest(
                "My Service", "desc",
                targetRoot.toString(),
                List.of(
                        new RepoConfig("https://github.com/owner/frontend", "ghp_a", "main", "frontend"),
                        new RepoConfig("https://github.com/owner/backend", "ghp_b", "develop", "backend")
                ),
                "https://staging.example.com"
        );
        QapilotService created = domain.create(request);

        assertThat(created.repos()).hasSize(2);
        assertThat(created.repos().get(0).repoUrl()).isEqualTo("https://github.com/owner/frontend");
        assertThat(created.repos().get(0).role()).isEqualTo("frontend");
        assertThat(created.repos().get(1).repoUrl()).isEqualTo("https://github.com/owner/backend");
        assertThat(created.repos().get(1).branch()).isEqualTo("develop");
        assertThat(created.stagingUrl()).isEqualTo("https://staging.example.com");
        assertThat(created.displayName()).isEqualTo("My Service");
    }

    @Test
    void createAcceptsNullRepos(@TempDir Path tempDir) throws Exception {
        Path targetRoot = tempDir.resolve("sut");
        Files.createDirectories(targetRoot);
        ServiceDomainService domain = newService(targetRoot);

        ServiceCreateRequest request = new ServiceCreateRequest(
                "Solo Service", null,
                targetRoot.toString(),
                null, null
        );
        QapilotService created = domain.create(request);

        assertThat(created.repos()).isNull();
        assertThat(created.stagingUrl()).isNull();
    }

    @Test
    void createDropsBlankRepoUrlEntries(@TempDir Path tempDir) throws Exception {
        Path targetRoot = tempDir.resolve("sut");
        Files.createDirectories(targetRoot);
        ServiceDomainService domain = newService(targetRoot);

        ServiceCreateRequest request = new ServiceCreateRequest(
                "Mixed", null,
                targetRoot.toString(),
                List.of(
                        new RepoConfig("  https://github.com/owner/keep  ", " ghp_x ", null, null),
                        new RepoConfig("", "ghp_drop", null, null),
                        new RepoConfig(null, null, null, null)
                ),
                null
        );
        QapilotService created = domain.create(request);

        assertThat(created.repos()).hasSize(1);
        assertThat(created.repos().get(0).repoUrl()).isEqualTo("https://github.com/owner/keep");
        assertThat(created.repos().get(0).token()).isEqualTo("ghp_x");
        assertThat(created.repos().get(0).branch()).isNull();
        assertThat(created.repos().get(0).role()).isNull();
    }

    @Test
    void createReturnsNullReposWhenAllBlank(@TempDir Path tempDir) throws Exception {
        Path targetRoot = tempDir.resolve("sut");
        Files.createDirectories(targetRoot);
        ServiceDomainService domain = newService(targetRoot);

        ServiceCreateRequest request = new ServiceCreateRequest(
                "All Blank", null,
                targetRoot.toString(),
                List.of(new RepoConfig("", "tok", null, null)),
                null
        );
        QapilotService created = domain.create(request);
        assertThat(created.repos()).isNull();
    }

    @Test
    void rotateTokenPreservesReposAndStagingUrl(@TempDir Path tempDir) throws Exception {
        Path targetRoot = tempDir.resolve("sut");
        Files.createDirectories(targetRoot);
        ServiceDomainService domain = newService(targetRoot);

        QapilotService created = domain.create(new ServiceCreateRequest(
                "Rotated", null, targetRoot.toString(),
                List.of(new RepoConfig("https://github.com/x/y", "tok", "dev", "core")),
                "https://staging"
        ));
        QapilotService rotated = domain.rotateToken(created.serviceId());

        assertThat(rotated.serverAuthToken()).isNotEqualTo(created.serverAuthToken());
        assertThat(rotated.repos()).hasSize(1);
        assertThat(rotated.repos().get(0).repoUrl()).isEqualTo("https://github.com/x/y");
        assertThat(rotated.stagingUrl()).isEqualTo("https://staging");
    }
}
