package com.qapilot.server.scenario;

import com.qapilot.server.scenario.change.ChangeRequestService;
import com.qapilot.server.service.persistence.ServiceJpaRepository;
import java.nio.file.Path;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 시나리오별 미해결 변경 요청 존재 여부를 도출. PR-15h — DB 기반.
 *
 * <p>호환 시그니처 (qapilotDir → slug → service_id) — PR-15 컷오버 중간 단계.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19, rewritten 2026-06-02
 */
@Component
public class ScenarioPendingChangesResolver {

    private final ChangeRequestService changeRequestService;
    private final ServiceJpaRepository serviceJpaRepository;

    public ScenarioPendingChangesResolver(
            ChangeRequestService changeRequestService,
            ServiceJpaRepository serviceJpaRepository
    ) {
        this.changeRequestService = changeRequestService;
        this.serviceJpaRepository = serviceJpaRepository;
    }

    public Set<String> scenariosWithPendingChanges(Path qapilotDir) {
        if (qapilotDir == null || qapilotDir.getFileName() == null) {
            return Set.of();
        }
        String slug = qapilotDir.getFileName().toString();
        return serviceJpaRepository.findBySlug(slug)
                .map(s -> changeRequestService.scenariosWithPendingChanges(s.getId().toString()))
                .orElse(Set.of());
    }
}
