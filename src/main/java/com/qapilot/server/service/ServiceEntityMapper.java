package com.qapilot.server.service;

import com.qapilot.server.service.domain.QapilotService;
import com.qapilot.server.service.domain.RepoConfig;
import com.qapilot.server.service.persistence.ServiceEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ServiceEntity (DB) → QapilotService (도메인 record) 변환.
 *
 * <p>PR-15g — ServiceDomainService 의 read 경로를 file → DB 로 갈아끼우기 위한 어댑터.
 * 응답 shape 의 키와 timestamp 포맷 모두 ServiceFileStore 출력과 동일하게 유지.
 * service_repos 조회는 다른 Reader 들과 일관성을 위해 native query 사용.
 *
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
@Component
public class ServiceEntityMapper {

    @PersistenceContext
    private EntityManager em;

    public QapilotService toDomain(ServiceEntity entity) {
        List<RepoConfig> repos = loadRepos(entity);
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
                repos.isEmpty() ? null : repos,
                entity.getStagingUrl()
        );
    }

    @SuppressWarnings("unchecked")
    private List<RepoConfig> loadRepos(ServiceEntity entity) {
        // Hibernate 6 + Postgres UUID native binding 회피 — string 으로 보내고 SQL에서 캐스팅.
        // (다른 Reader 들은 EntityManager 가 트랜잭션 scope 안에서 UUID 객체 받아 작동하지만, 본 호출은
        //  Service 메서드 밖의 read-only 컨텍스트에서 발생할 수 있어 안전한 경로 선택)
        List<Object[]> rows = em.createNativeQuery(
                "SELECT repo_url, branch, role, token " +
                "FROM service_repos WHERE service_id = CAST(:svc AS uuid) " +
                "ORDER BY position ASC"
        ).setParameter("svc", entity.getId().toString()).getResultList();
        List<RepoConfig> repos = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            repos.add(new RepoConfig((String) r[0], (String) r[3], (String) r[1], (String) r[2]));
        }
        return repos;
    }

    private String toIso(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
