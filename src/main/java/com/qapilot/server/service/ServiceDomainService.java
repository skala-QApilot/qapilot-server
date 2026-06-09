package com.qapilot.server.service;

import com.qapilot.server.common.config.QapilotProperties;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.QapilotPathResolver;
import com.qapilot.server.organization.OrganizationService;
import com.qapilot.server.organization.domain.Organization;
import com.qapilot.server.service.domain.QapilotService;
import com.qapilot.server.service.domain.RepoConfig;
import com.qapilot.server.service.dto.CredentialsResponse;
import com.qapilot.server.service.dto.ProjectDashboardResponse;
import com.qapilot.server.service.dto.ProjectSummaryResponse;
import com.qapilot.server.service.dto.ServiceCreateRequest;
import com.qapilot.server.service.dto.ServiceResponse;
import com.qapilot.server.service.dto.ServiceSetupRequest;
import com.qapilot.server.service.dto.ServiceUpdateRequest;
import com.qapilot.server.service.persistence.ServiceEntity;
import com.qapilot.server.service.persistence.ServiceJpaRepository;
import com.qapilot.server.service.persistence.ServiceRepoEntity;
import com.qapilot.server.service.persistence.ServiceRepoJpaRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final ServiceSlugGenerator slugGenerator;
    private final QapilotPathResolver pathResolver;
    private final QapilotProperties properties;
    private final ServiceJpaRepository serviceJpaRepository;
    private final ServiceRepoJpaRepository serviceRepoJpaRepository;
    private final ServiceEntityMapper serviceEntityMapper;
    private final OrganizationService organizationService;

    public ServiceDomainService(
            ServiceSlugGenerator slugGenerator,
            QapilotPathResolver pathResolver,
            QapilotProperties properties,
            ServiceJpaRepository serviceJpaRepository,
            ServiceRepoJpaRepository serviceRepoJpaRepository,
            ServiceEntityMapper serviceEntityMapper,
            OrganizationService organizationService
    ) {
        this.slugGenerator = slugGenerator;
        this.pathResolver = pathResolver;
        this.properties = properties;
        this.serviceJpaRepository = serviceJpaRepository;
        this.serviceRepoJpaRepository = serviceRepoJpaRepository;
        this.serviceEntityMapper = serviceEntityMapper;
        this.organizationService = organizationService;
    }

    public List<ServiceResponse> listServices() {
        // PR-15g — DB 가 read 의 source of truth. services.json 은 dual-write 잔존 (PR-15h 정리).
        return serviceJpaRepository.findAll().stream()
                .map(serviceEntityMapper::toDomain)
                .map(ServiceResponse::from)
                .toList();
    }

    public QapilotService create(ServiceCreateRequest request, UUID userId) {
        String name = requiredName(request.name());
        Path targetRoot = targetRoot(request.targetRoot());
        if (!Files.exists(targetRoot) || !Files.isDirectory(targetRoot)) {
            throw new QapilotException(ErrorCode.SERVICE_002, "대상 경로를 찾을 수 없습니다: " + targetRoot);
        }

        // PR-15h — slug 충돌 체크 DB 기반.
        String slug = ensureUniqueSlug(slugGenerator.normalize(name));
        Path qapilotDir = pathResolver.qapilotDir(targetRoot, slug);
        createRequiredDirs(qapilotDir);

        UUID orgId = resolvePersonalOrgId(userId);
        if (orgId == null) {
            throw new QapilotException(ErrorCode.SERVICE_001, "userId 의 organization 을 찾을 수 없습니다.");
        }

        UUID serviceId = UUID.randomUUID();
        Instant nowInstant = Instant.now();
        ServiceEntity entity = new ServiceEntity();
        entity.setId(serviceId);
        entity.setOrgId(orgId);
        entity.setSlug(slug);
        entity.setDisplayName(name);
        entity.setDescription(request.description() == null ? "" : request.description());
        entity.setTargetRoot(targetRoot.toString());
        entity.setQapilotDir(qapilotDir.toString());
        entity.setDashboardUrl(dashboardUrl(slug));
        entity.setServerAuthToken(generateServerAuthToken());
        entity.setTokenIssuedAt(nowInstant);
        entity.setStagingUrl(nullIfBlank(request.stagingUrl()));
        serviceJpaRepository.save(entity);

        List<RepoConfig> normalizedRepos = normalizeRepos(request.repos());
        if (normalizedRepos != null) {
            int position = 0;
            for (RepoConfig repo : normalizedRepos) {
                ServiceRepoEntity repoEntity = new ServiceRepoEntity();
                repoEntity.setId(UUID.randomUUID());
                repoEntity.setServiceId(serviceId);
                repoEntity.setRepoUrl(repo.repoUrl());
                repoEntity.setBranch(repo.branch());
                repoEntity.setRole(repo.role());
                repoEntity.setToken(repo.token());
                repoEntity.setPosition(position++);
                serviceRepoJpaRepository.save(repoEntity);
            }
        }

        return serviceEntityMapper.toDomain(entity);
    }

    /** baseSlug 와 같은 slug 가 DB 에 있으면 -2, -3... suffix 부여. */
    private String ensureUniqueSlug(String baseSlug) {
        if (serviceJpaRepository.findBySlug(baseSlug).isEmpty()) {
            return baseSlug;
        }
        int suffix = 2;
        while (serviceJpaRepository.findBySlug(baseSlug + "-" + suffix).isPresent()) {
            suffix++;
        }
        return baseSlug + "-" + suffix;
    }

    private UUID resolvePersonalOrgId(UUID userId) {
        List<Organization> orgs = organizationService.listForUser(userId);
        return orgs.isEmpty() ? null : orgs.get(0).getId();
    }

    private static String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 입력 repos 를 정규화한다 — blank repo_url 인 entry 는 drop, 나머지 필드는 trim.
     * 결과가 비면 null 반환 (services.json 에 빈 배열 대신 키 자체 생략).
     */
    private static List<RepoConfig> normalizeRepos(List<RepoConfig> repos) {
        if (repos == null || repos.isEmpty()) {
            return null;
        }
        List<RepoConfig> cleaned = repos.stream()
                .filter(r -> r != null && r.repoUrl() != null && !r.repoUrl().isBlank())
                .map(r -> new RepoConfig(
                        r.repoUrl().trim(),
                        nullIfBlank(r.token()),
                        nullIfBlank(r.branch()),
                        nullIfBlank(r.role())
                ))
                .toList();
        return cleaned.isEmpty() ? null : cleaned;
    }

    public QapilotService getById(String serviceId) {
        try {
            return serviceJpaRepository.findById(UUID.fromString(serviceId))
                    .map(serviceEntityMapper::toDomain)
                    .orElseThrow(() -> new QapilotException(ErrorCode.SERVICE_001));
        } catch (IllegalArgumentException e) {
            throw new QapilotException(ErrorCode.SERVICE_001);
        }
    }

    public QapilotService getByProjectSlug(String projectSlug) {
        return serviceJpaRepository.findBySlug(projectSlug)
                .map(serviceEntityMapper::toDomain)
                .orElseThrow(() -> new QapilotException(ErrorCode.SERVICE_001));
    }

    @Transactional
    public QapilotService update(String serviceId, ServiceUpdateRequest request) {
        UUID id = UUID.fromString(serviceId);
        ServiceEntity entity = serviceJpaRepository.findById(id)
                .orElseThrow(() -> new QapilotException(ErrorCode.SERVICE_001));
        if (request.name() != null && !request.name().isBlank()) {
            entity.setDisplayName(request.name().trim());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        if (request.stagingUrl() != null) {
            entity.setStagingUrl(nullIfBlank(request.stagingUrl()));
        }
        serviceJpaRepository.save(entity);

        if (request.repos() != null) {
            replaceRepos(id, request.repos());
        }
        return serviceEntityMapper.toDomain(entity);
    }

    /**
     * service_repos 를 incoming 으로 교체한다. token 이 blank 인 repo 는 동일 repo_url 의
     * 기존 token 을 보존한다 — 설정 UI 는 PAT 를 token_set 으로만 받아 재전송하지 않으므로,
     * blank = "변경 없음" 으로 해석한다.
     */
    private void replaceRepos(UUID serviceId, List<RepoConfig> incoming) {
        List<ServiceRepoEntity> existing =
                serviceRepoJpaRepository.findAllByServiceIdOrderByPositionAsc(serviceId);
        Map<String, String> tokenByUrl = new HashMap<>();
        for (ServiceRepoEntity e : existing) {
            if (e.getToken() != null) {
                tokenByUrl.put(e.getRepoUrl(), e.getToken());
            }
        }
        serviceRepoJpaRepository.deleteAll(existing);
        serviceRepoJpaRepository.flush();

        List<RepoConfig> normalized = normalizeRepos(incoming);
        if (normalized == null) {
            return;
        }
        int position = 0;
        for (RepoConfig repo : normalized) {
            String token = repo.token();
            if (token == null) {                       // blank → 기존 PAT 보존
                token = tokenByUrl.get(repo.repoUrl());
            }
            ServiceRepoEntity repoEntity = new ServiceRepoEntity();
            repoEntity.setId(UUID.randomUUID());
            repoEntity.setServiceId(serviceId);
            repoEntity.setRepoUrl(repo.repoUrl());
            repoEntity.setBranch(repo.branch());
            repoEntity.setRole(repo.role());
            repoEntity.setToken(token);
            repoEntity.setPosition(position++);
            serviceRepoJpaRepository.save(repoEntity);
        }
    }

    public QapilotService setup(String serviceId, ServiceSetupRequest request) {
        QapilotService service = getById(serviceId);
        createRequiredDirs(Path.of(service.qapilotDir()));
        if (request.description() == null) {
            return service;
        }
        return update(serviceId, new ServiceUpdateRequest(service.displayName(), request.description(), null, null));
    }

    public CredentialsResponse credentials(String serviceId) {
        return CredentialsResponse.from(getById(serviceId));
    }

    public QapilotService rotateToken(String serviceId) {
        ServiceEntity entity = serviceJpaRepository.findById(UUID.fromString(serviceId))
                .orElseThrow(() -> new QapilotException(ErrorCode.SERVICE_001));
        entity.setServerAuthToken(generateServerAuthToken());
        entity.setTokenIssuedAt(Instant.now());
        entity.setTokenExpiresAt(null);
        serviceJpaRepository.save(entity);
        return serviceEntityMapper.toDomain(entity);
    }
    
    @Transactional
    public void delete(String serviceId) {
        UUID id;
        try {
            id = UUID.fromString(serviceId);
        } catch (IllegalArgumentException e) {
            throw new QapilotException(ErrorCode.SERVICE_001);
        }
        ServiceEntity entity = serviceJpaRepository.findById(id)
                .orElseThrow(() -> new QapilotException(ErrorCode.SERVICE_001));
        serviceRepoJpaRepository.deleteAllByServiceId(id);
        serviceJpaRepository.delete(entity);
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
        return serviceJpaRepository.findBySlug(projectSlug)
                .map(ServiceEntity::getServerAuthToken)
                .filter(stored -> stored.equals(token))
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

}
