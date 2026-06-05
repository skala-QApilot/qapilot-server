package com.qapilot.server.dashboard;

import com.qapilot.server.dashboard.dto.DashboardSummaryResponse;
import com.qapilot.server.file.persistence.DomainDocumentEntity;
import com.qapilot.server.file.persistence.DomainDocumentRepository;
import com.qapilot.server.run.RunReader;
import com.qapilot.server.run.dto.RunResponse;
import com.qapilot.server.scenario.ScenarioReader;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final DomainDocumentRepository domainDocumentRepository;

    public DashboardService(
            ServiceDomainService serviceDomainService,
            ScenarioReader scenarioReader,
            RunReader runReader,
            DomainDocumentRepository domainDocumentRepository
    ) {
        this.serviceDomainService = serviceDomainService;
        this.scenarioReader = scenarioReader;
        this.runReader = runReader;
        this.domainDocumentRepository = domainDocumentRepository;
    }

    public DashboardSummaryResponse summary(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        List<Map<String, Object>> traces = runReader.listByServiceId(service.serviceId());
        List<RunResponse> recentRuns = traces.stream().limit(5).map(RunResponse::fromTrace).toList();
        return new DashboardSummaryResponse(
                scenarioReader.listByServiceId(serviceId).size(),
                recentRuns,
                domainFiles(serviceId),
                null,
                passRate(traces)
        );
    }

    private List<String> domainFiles(String serviceId) {
        UUID svc;
        try {
            svc = UUID.fromString(serviceId);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        return domainDocumentRepository.findAllByServiceIdOrderByUploadedAtDesc(svc).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> e.getFileId() != null ? e.getFileId() : e.getId()))
                .values().stream()
                .map(group -> group.stream().max(java.util.Comparator.comparingInt(DomainDocumentEntity::getVersion))
                        .orElseThrow())
                .map(DomainDocumentEntity::getFilename)
                .sorted()
                .toList();
    }

    private Double passRate(List<Map<String, Object>> traces) {
        return null;
    }
}
