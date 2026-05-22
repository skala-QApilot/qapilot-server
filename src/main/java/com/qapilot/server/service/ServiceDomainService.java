package com.qapilot.server.service;

import com.qapilot.server.common.config.QapilotProperties;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.QapilotPathResolver;
import com.qapilot.server.service.domain.QapilotService;
import com.qapilot.server.service.dto.CredentialsResponse;
import com.qapilot.server.service.dto.ProjectDashboardResponse;
import com.qapilot.server.service.dto.ProjectSummaryResponse;
import com.qapilot.server.service.dto.ServiceCreateRequest;
import com.qapilot.server.service.dto.ServiceResponse;
import com.qapilot.server.service.dto.ServiceSetupRequest;
import com.qapilot.server.service.dto.ServiceUpdateRequest;
import com.qapilot.server.service.store.ServiceFileStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 서비스 도메인 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class ServiceDomainService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final List<String> REQUIRED_DIRS = List.of(
            "auth", "cache", "codebase-index", "domain", "evidence",
            "generated-code", "logs", "reports", "results", "scenarios", "traces"
    );

    private final ServiceFileStore serviceFileStore;
    private final ServiceSlugGenerator slugGenerator;
    private final QapilotPathResolver pathResolver;
    private final QapilotProperties properties;

    public ServiceDomainService(
            ServiceFileStore serviceFileStore,
            ServiceSlugGenerator slugGenerator,
            QapilotPathResolver pathResolver,
            QapilotProperties properties
    ) {
        this.serviceFileStore = serviceFileStore;
        this.slugGenerator = slugGenerator;
        this.pathResolver = pathResolver;
        this.properties = properties;
    }

    public List<ServiceResponse> listServices() {
        return serviceFileStore.loadServices().stream().map(ServiceResponse::from).toList();
    }

    public QapilotService create(ServiceCreateRequest request) {
        String name = requiredName(request.name());
        Path targetRoot = targetRoot(request.targetRoot());
        if (!Files.exists(targetRoot) || !Files.isDirectory(targetRoot)) {
            throw new QapilotException(ErrorCode.SERVICE_002, "대상 경로를 찾을 수 없습니다: " + targetRoot);
        }

        List<QapilotService> services = serviceFileStore.loadServices();
        String now = now();
        String slug = slugGenerator.generate(name, services);
        Path qapilotDir = pathResolver.qapilotDir(targetRoot);
        createRequiredDirs(qapilotDir);

        QapilotService service = new QapilotService(
                UUID.randomUUID().toString(),
                slug,
                name,
                request.description() == null ? "" : request.description(),
                targetRoot.toString(),
                qapilotDir.toString(),
                dashboardUrl(slug),
                generateServerAuthToken(),
                now,
                null,
                now,
                now,
                nullIfBlank(request.repoUrl()),
                nullIfBlank(request.repoToken()),
                nullIfBlank(request.repoBranch())
        );
        services.add(service);
        serviceFileStore.saveServices(services);
        return service;
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public QapilotService getById(String serviceId) {
        return serviceFileStore.findById(serviceId)
                .orElseThrow(() -> new QapilotException(ErrorCode.SERVICE_001));
    }

    public QapilotService getByProjectSlug(String projectSlug) {
        return serviceFileStore.findByProjectSlug(projectSlug)
                .orElseThrow(() -> new QapilotException(ErrorCode.SERVICE_001));
    }

    public QapilotService update(String serviceId, ServiceUpdateRequest request) {
        List<QapilotService> services = serviceFileStore.loadServices();
        QapilotService current = services.stream()
                .filter(service -> service.serviceId().equals(serviceId))
                .findFirst()
                .orElseThrow(() -> new QapilotException(ErrorCode.SERVICE_001));
        QapilotService updated = copyWithMutableFields(current, request.name(), request.description(), now());
        serviceFileStore.saveServices(replaceService(services, updated));
        return updated;
    }

    public QapilotService setup(String serviceId, ServiceSetupRequest request) {
        QapilotService service = getById(serviceId);
        createRequiredDirs(Path.of(service.qapilotDir()));
        if (request.description() == null) {
            return service;
        }
        return update(serviceId, new ServiceUpdateRequest(service.displayName(), request.description()));
    }

    public CredentialsResponse credentials(String serviceId) {
        return CredentialsResponse.from(getById(serviceId));
    }

    public QapilotService rotateToken(String serviceId) {
        List<QapilotService> services = serviceFileStore.loadServices();
        QapilotService current = getById(serviceId);
        QapilotService rotated = new QapilotService(
                current.serviceId(), current.projectSlug(), current.displayName(), current.description(),
                current.targetRoot(), current.qapilotDir(), current.dashboardUrl(), generateServerAuthToken(),
                now(), null, current.createdAt(), now(),
                current.repoUrl(), current.repoToken(), current.repoBranch()
        );
        serviceFileStore.saveServices(replaceService(services, rotated));
        return rotated;
    }

    public ProjectDashboardResponse projectDashboard(String projectSlug) {
        QapilotService service = getByProjectSlug(projectSlug);
        return new ProjectDashboardResponse(
                ServiceResponse.from(service),
                summary(service),
                CredentialsResponse.from(service)
        );
    }

    public boolean tokenMatches(String projectSlug, String token) {
        return serviceFileStore.findByProjectSlug(projectSlug)
                .filter(service -> service.serverAuthToken().equals(token))
                .isPresent();
    }

    public QapilotService serviceByProjectSlugAndToken(String projectSlug, String token) {
        QapilotService service = getByProjectSlug(projectSlug);
        if (!service.serverAuthToken().equals(token)) {
            throw new QapilotException(ErrorCode.SERVICE_003);
        }
        return service;
    }

    private ProjectSummaryResponse summary(QapilotService service) {
        Path targetRoot = Path.of(service.targetRoot());
        Path indexPath = Path.of(service.qapilotDir()).resolve("codebase-index");
        int fileCount = countFiles(targetRoot);
        String updatedAt = service.updatedAt() == null ? service.createdAt() : service.updatedAt();
        return new ProjectSummaryResponse(fileCount, 0, 0, updatedAt, Files.isDirectory(indexPath), List.of());
    }

    private int countFiles(Path root) {
        try (var stream = Files.walk(root)) {
            return (int) stream.filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains(".qapilot"))
                    .count();
        } catch (IOException e) {
            return 0;
        }
    }

    private String requiredName(String name) {
        if (name == null || name.isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "name 필드가 필요합니다.");
        }
        return name.trim();
    }

    private Path targetRoot(String targetRoot) {
        String value = targetRoot == null || targetRoot.isBlank()
                ? properties.storage().defaultTargetRoot()
                : targetRoot;
        return Path.of(value).toAbsolutePath().normalize();
    }

    private String dashboardUrl(String projectSlug) {
        return "http://localhost:8080/" + projectSlug;
    }

    private String generateServerAuthToken() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return "qap_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void createRequiredDirs(Path qapilotDir) {
        try {
            for (String dir : REQUIRED_DIRS) {
                Files.createDirectories(qapilotDir.resolve(dir));
            }
        } catch (IOException e) {
            throw new QapilotException(ErrorCode.FILE_001, ".qapilot 디렉터리 구조를 생성할 수 없습니다.");
        }
    }

    private List<QapilotService> replaceService(List<QapilotService> services, QapilotService updated) {
        return services.stream()
                .map(service -> service.serviceId().equals(updated.serviceId()) ? updated : service)
                .toList();
    }

    private QapilotService copyWithMutableFields(
            QapilotService current,
            String name,
            String description,
            String updatedAt
    ) {
        return new QapilotService(
                current.serviceId(),
                current.projectSlug(),
                name == null || name.isBlank() ? current.displayName() : name.trim(),
                description == null ? current.description() : description,
                current.targetRoot(),
                current.qapilotDir(),
                current.dashboardUrl(),
                current.serverAuthToken(),
                current.tokenIssuedAt(),
                current.tokenExpiresAt(),
                current.createdAt(),
                updatedAt,
                current.repoUrl(),
                current.repoToken(),
                current.repoBranch()
        );
    }

    private String now() {
        return Instant.now().toString();
    }
}
