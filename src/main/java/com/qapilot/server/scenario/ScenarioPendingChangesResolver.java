package com.qapilot.server.scenario;

import com.qapilot.server.scenario.change.ChangeRequestFileStore;
import com.qapilot.server.scenario.change.domain.ChangeRequest;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 시나리오별 미해결 변경 요청 존재 여부를 도출한다.
 *
 * <p>변경 요청은 reviewer 가 approved / rejected 로 닫으면 종결. 그 외 상태
 * (null / "pending" / "deferred") 는 모두 "미해결" 로 본다 — 시나리오 카드에서
 * "변경 인디케이터" 표시 대상.
 *
 * <p>Phase D (DB) 진화 시: file 순회 → `change_requests` 테이블의
 * `WHERE scenario_id = ? AND status NOT IN ('approved','rejected')` 쿼리로 자연 교체.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
@Component
public class ScenarioPendingChangesResolver {

    private static final Set<String> RESOLVED_STATUSES = Set.of("approved", "rejected");

    private final ChangeRequestFileStore changeRequestFileStore;

    public ScenarioPendingChangesResolver(ChangeRequestFileStore changeRequestFileStore) {
        this.changeRequestFileStore = changeRequestFileStore;
    }

    /**
     * 미해결 변경 요청이 존재하는 시나리오 id 집합.
     */
    public Set<String> scenariosWithPendingChanges(Path qapilotDir) {
        Set<String> result = new HashSet<>();
        for (ChangeRequest req : changeRequestFileStore.listAll(qapilotDir)) {
            if (req.scenarioId() == null || req.scenarioId().isBlank()) {
                continue;
            }
            if (isResolved(req.status())) {
                continue;
            }
            result.add(req.scenarioId());
        }
        return result;
    }

    private boolean isResolved(String status) {
        return status != null && RESOLVED_STATUSES.contains(status);
    }
}
