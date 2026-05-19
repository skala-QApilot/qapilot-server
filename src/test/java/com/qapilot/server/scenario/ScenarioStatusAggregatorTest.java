package com.qapilot.server.scenario;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.trace.TraceFileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * ScenarioStatusAggregator 단위 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
class ScenarioStatusAggregatorTest {

    @TempDir
    Path qapilotDir;

    private final JsonFileStore jsonFileStore = new JsonFileStore(new ObjectMapper());
    private final TraceFileStore traceFileStore = new TraceFileStore(jsonFileStore);
    private final ScenarioStatusAggregator aggregator = new ScenarioStatusAggregator(traceFileStore);

    @Test
    void emptyWhenNoTraces() throws Exception {
        assertThat(aggregator.scenarioStatuses(qapilotDir)).isEmpty();
        assertThat(aggregator.testCaseStatuses(qapilotDir)).isEmpty();
    }

    @Test
    void emptyWhenTracesHaveNoStatusMaps() throws Exception {
        writeTrace("trace-1", "2026-05-19T00:00:00Z", "2026-05-19T00:01:00Z", null, null);
        assertThat(aggregator.scenarioStatuses(qapilotDir)).isEmpty();
        assertThat(aggregator.testCaseStatuses(qapilotDir)).isEmpty();
    }

    @Test
    void usesLatestTraceForEachScenario() throws Exception {
        // 가장 오래된 trace
        writeTrace(
                "trace-old",
                "2026-05-19T00:00:00Z",
                "2026-05-19T00:01:00Z",
                Map.of("TS-001", "failed", "TS-002", "passed"),
                Map.of("TC-001", "failed")
        );
        // 가장 최근 trace — started_at 이 더 늦음
        writeTrace(
                "trace-new",
                "2026-05-19T02:00:00Z",
                "2026-05-19T02:01:00Z",
                Map.of("TS-001", "passed"),
                Map.of("TC-001", "passed", "TC-002", "passed")
        );

        Map<String, ScenarioStatusAggregator.RunStatus> scenarios = aggregator.scenarioStatuses(qapilotDir);
        assertThat(scenarios).hasSize(2);
        assertThat(scenarios.get("TS-001").status()).isEqualTo("passed");
        assertThat(scenarios.get("TS-001").at()).isEqualTo("2026-05-19T02:01:00Z");
        assertThat(scenarios.get("TS-002").status()).isEqualTo("passed");
        assertThat(scenarios.get("TS-002").at()).isEqualTo("2026-05-19T00:01:00Z");

        Map<String, ScenarioStatusAggregator.RunStatus> tcs = aggregator.testCaseStatuses(qapilotDir);
        assertThat(tcs.get("TC-001").status()).isEqualTo("passed");
        assertThat(tcs.get("TC-002").status()).isEqualTo("passed");
    }

    @Test
    void fallsBackToStartedAtWhenCompletedAtMissing() throws Exception {
        writeTrace(
                "trace-running",
                "2026-05-19T03:00:00Z",
                null,
                Map.of("TS-001", "passed"),
                Map.of()
        );
        Map<String, ScenarioStatusAggregator.RunStatus> scenarios = aggregator.scenarioStatuses(qapilotDir);
        assertThat(scenarios.get("TS-001").at()).isEqualTo("2026-05-19T03:00:00Z");
    }

    private void writeTrace(
            String traceId,
            String startedAt,
            String completedAt,
            Map<String, String> scenarioResults,
            Map<String, String> tcResults
    ) throws Exception {
        Path tracesDir = qapilotDir.resolve("traces");
        Files.createDirectories(tracesDir);
        StringBuilder json = new StringBuilder("{");
        json.append("\"trace_id\":\"").append(traceId).append("\",");
        json.append("\"command\":\"test\",");
        json.append("\"status\":\"completed\",");
        json.append("\"started_at\":\"").append(startedAt).append("\"");
        if (completedAt != null) {
            json.append(",\"completed_at\":\"").append(completedAt).append("\"");
        }
        if (scenarioResults != null) {
            json.append(",\"scenario_results\":").append(toJsonMap(scenarioResults));
        }
        if (tcResults != null) {
            json.append(",\"tc_results\":").append(toJsonMap(tcResults));
        }
        json.append("}");
        Files.writeString(tracesDir.resolve(traceId + ".json"), json.toString());
    }

    private String toJsonMap(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
