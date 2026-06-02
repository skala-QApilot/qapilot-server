package com.qapilot.server.service;

import com.qapilot.server.service.domain.QapilotService;
import com.qapilot.server.service.domain.RepoConfig;
import com.qapilot.server.service.persistence.ServiceEntity;
import com.qapilot.server.service.persistence.ServiceRepoEntity;
import com.qapilot.server.service.persistence.ServiceRepoJpaRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ServiceEntity (DB) → QapilotService (도메인 record) 변환.
 *
 * <p>PR-15g — ServiceDomainService 의 read 경로를 file → DB 로 갈아끼우기 위한 어댑터.
 * 응답 shape 의 키와 timestamp 포맷 모두 ServiceFileStore 출력과 동일하게 유지.
 *
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
@Component
public class ServiceEntityMapper {

    private final ServiceRepoJpaRepository serviceRepoJpaRepository;

    public ServiceEntityMapper(ServiceRepoJpaRepository serviceRepoJpaRepository) {
        this.serviceRepoJpaRepository = serviceRepoJpaRepository;
    }

    public QapilotService toDomain(ServiceEntity entity) {
        List<ServiceRepoEntity> repoRows = serviceRepoJpaRepository
                .findAllByServiceIdOrderByPositionAsc(entity.getId());
        List<RepoConfig> repos = repoRows.isEmpty() ? null : repoRows.stream()
                .map(this::toRepoConfig)
                .toList();
        return new QapilotService(
                entity.getId().toString(),
                entity.getSlug(),
                entity.getDisplayName(),
                entity.getDescription() == null ? "" : entity.getDescription(),
                entity.getTargetRoot(),
                entity.getQapilotDir(),
                entity.getDashboardUrl(),
                entity.getServerAuthToken(),
                toIso(entity.getTokenIssuedAt()),
                toIso(entity.getTokenExpiresAt()),
                toIso(entity.getCreatedAt()),
                toIso(entity.getUpdatedAt()),
                repos,
                entity.getStagingUrl()
        );
    }

    private RepoConfig toRepoConfig(ServiceRepoEntity e) {
        return new RepoConfig(e.getRepoUrl(), e.getToken(), e.getBranch(), e.getRole());
    }

    private String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
