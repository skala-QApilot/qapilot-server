package com.qapilot.server.dashboard;

import com.qapilot.server.dashboard.dto.DashboardSummaryResponse;
import com.qapilot.server.run.RunReader;
import com.qapilot.server.run.dto.RunResponse;
import com.qapilot.server.scenario.ScenarioReader;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 대시보드 집계 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class DashboardService {

    private final ServiceDomainService serviceDomainService;
    private final ScenarioReader scenarioReader;
    private final RunReader runReader;

    public DashboardService(
            ServiceDomainService serviceDomainService,
            ScenarioReader scenarioReader,
            RunReader runReader
    ) {
        this.serviceDomainService = serviceDomainService;
        this.scenarioReader = scenarioReader;
        this.runReader = runReader;
    }

    public DashboardSummaryResponse summary(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        Path qapilotDir = Path.of(service.qapilotDir());
        List<Map<String, Object>> traces = runReader.listByServiceId(service.serviceId());
        List<RunResponse> recentRuns = traces.stream().limit(5).map(RunResponse::fromTrace).toList();
        return new DashboardSummaryResponse(
                scenarioReader.listByServiceId(serviceId).size(),
                recentRuns,
                domainFiles(qapilotDir),
                null,
                passRate(traces)
        );
    }

    private List<String> domainFiles(Path qapilotDir) {
        Path domainDir = qapilotDir.resolve("domain");
        if (!Files.isDirectory(domainDir)) {
            return List.of();
        }
        try (var stream = Files.list(domainDir)) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private Double passRate(List<Map<String, Object>> traces) {
        return null;
    }
}
