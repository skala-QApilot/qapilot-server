package com.qapilot.server.scenario;

import com.qapilot.server.scenario.change.ChangeRequestService;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 시나리오별 미해결 변경 요청 존재 여부를 도출. PR-15h — DB 기반.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19, rewritten 2026-06-02
 */
@Component
public class ScenarioPendingChangesResolver {

    private final ChangeRequestService changeRequestService;

    public ScenarioPendingChangesResolver(ChangeRequestService changeRequestService) {
        this.changeRequestService = changeRequestService;
    }

    public Set<String> scenariosWithPendingChanges(String serviceId) {
        if (serviceId == null || serviceId.isBlank()) {
            return Set.of();
        }
        return changeRequestService.scenariosWithPendingChanges(serviceId);
    }
}
