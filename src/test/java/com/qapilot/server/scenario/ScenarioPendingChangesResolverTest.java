package com.qapilot.server.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.scenario.change.ChangeRequestFileStore;
import com.qapilot.server.scenario.change.domain.ChangeRequest;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ScenarioPendingChangesResolver 단위 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
class ScenarioPendingChangesResolverTest {

    @TempDir
    Path qapilotDir;

    private final JsonFileStore jsonFileStore = new JsonFileStore(new ObjectMapper());
    private final ChangeRequestFileStore changeRequestFileStore = new ChangeRequestFileStore(jsonFileStore);
    private final ScenarioPendingChangesResolver resolver =
            new ScenarioPendingChangesResolver(changeRequestFileStore);

    @Test
    void emptyWhenNoChangeRequests() {
        assertThat(resolver.scenariosWithPendingChanges(qapilotDir)).isEmpty();
    }

    @Test
    void countsPendingAndDeferredAsUnresolved() {
        save("req-1", "TS-001", null);            // 미리뷰
        save("req-2", "TS-002", "pending");        // 명시적 pending
        save("req-3", "TS-003", "deferred");      // 보류
        save("req-4", "TS-004", "approved");      // 종결
        save("req-5", "TS-005", "rejected");      // 종결

        Set<String> pending = resolver.scenariosWithPendingChanges(qapilotDir);

        assertThat(pending).containsExactlyInAnyOrder("TS-001", "TS-002", "TS-003");
    }

    @Test
    void mixedRequestsForSameScenario_anyPendingMarksIt() {
        save("req-1", "TS-001", "approved");
        save("req-2", "TS-001", null);  // 같은 시나리오의 미해결 요청

        assertThat(resolver.scenariosWithPendingChanges(qapilotDir)).containsExactly("TS-001");
    }

    @Test
    void skipsBlankScenarioId() {
        save("req-1", "", null);
        save("req-2", null, null);

        assertThat(resolver.scenariosWithPendingChanges(qapilotDir)).isEmpty();
    }

    private void save(String requestId, String scenarioId, String status) {
        changeRequestFileStore.save(
                qapilotDir,
                new ChangeRequest(
                        requestId,
                        scenarioId,
                        "이유",
                        "chatbot",
                        status,
                        "2026-05-19T00:00:00Z",
                        "2026-05-19T00:00:00Z",
                        null,
                        null
                )
        );
    }
}
