package com.qapilot.server.scenario.change;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.scenario.change.domain.ChangeRequest;
import com.qapilot.server.scenario.change.dto.UpdateChangeRequestRequest;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 변경 요청 관리 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class ChangeRequestService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("approved", "deferred", "rejected");

    private final ServiceDomainService serviceDomainService;
    private final ChangeRequestFileStore changeRequestFileStore;

    public ChangeRequestService(ServiceDomainService serviceDomainService, ChangeRequestFileStore changeRequestFileStore) {
        this.serviceDomainService = serviceDomainService;
        this.changeRequestFileStore = changeRequestFileStore;
    }

    public List<ChangeRequest> list(String serviceId, String status, String trigger) {
        return changeRequestFileStore.listAll(qapilotDir(serviceId)).stream()
                .filter(req -> status == null || status.equals(req.status()))
                .filter(req -> trigger == null || trigger.equals(req.trigger()))
                .toList();
    }

    public ChangeRequest update(String serviceId, String requestId, UpdateChangeRequestRequest request) {
        if (request.status() != null && !ALLOWED_STATUSES.contains(request.status())) {
            throw new QapilotException(ErrorCode.CHANGE_REQUEST_002);
        }
        Path qapilotDir = qapilotDir(serviceId);
        ChangeRequest current = changeRequestFileStore.load(qapilotDir, requestId);
        String now = Instant.now().toString();
        ChangeRequest updated = new ChangeRequest(
                current.requestId(),
                current.scenarioId(),
                current.reason(),
                current.trigger(),
                request.status() != null ? request.status() : current.status(),
                current.createdAt(),
                now,
                request.status() != null ? now : current.reviewedAt(),
                request.reviewer() != null ? request.reviewer() : current.reviewer()
        );
        changeRequestFileStore.save(qapilotDir, updated);
        return updated;
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
    }
}
