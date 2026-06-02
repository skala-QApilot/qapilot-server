package com.qapilot.server.scenario.change;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.scenario.change.domain.ChangeRequest;
import com.qapilot.server.scenario.change.dto.UpdateChangeRequestRequest;
import com.qapilot.server.scenario.change.persistence.ChangeRequestEntity;
import com.qapilot.server.scenario.change.persistence.ChangeRequestRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 변경 요청 관리 유스케이스. PR-15h — JPA only.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18, rewritten 2026-06-02
 */
@Service
public class ChangeRequestService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("approved", "deferred", "rejected");

    private final ChangeRequestRepository changeRequestRepository;

    public ChangeRequestService(ChangeRequestRepository changeRequestRepository) {
        this.changeRequestRepository = changeRequestRepository;
    }

    public List<ChangeRequest> list(String serviceId, String status, String trigger) {
        return changeRequestRepository.findAllByServiceIdOrderByCreatedAtDesc(UUID.fromString(serviceId)).stream()
                .filter(req -> status == null || status.equals(req.getStatus()))
                .filter(req -> trigger == null || trigger.equals(req.getTrigger()))
                .map(this::toDomain)
                .toList();
    }

    public ChangeRequest update(String serviceId, String requestId, UpdateChangeRequestRequest request) {
        if (request.status() != null && !ALLOWED_STATUSES.contains(request.status())) {
            throw new QapilotException(ErrorCode.CHANGE_REQUEST_002);
        }
        ChangeRequestEntity entity = changeRequestRepository.findById(UUID.fromString(requestId))
                .orElseThrow(() -> new QapilotException(ErrorCode.CHANGE_REQUEST_001));
        if (!entity.getServiceId().equals(UUID.fromString(serviceId))) {
            throw new QapilotException(ErrorCode.CHANGE_REQUEST_001);
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
            entity.setReviewedAt(Instant.now());
        }
        if (request.reviewer() != null) {
            entity.setReviewer(request.reviewer());
        }
        changeRequestRepository.save(entity);
        return toDomain(entity);
    }

    /** 미해결 change_request 가 있는 시나리오 id 집합 — ScenarioPendingChangesResolver 가 호출. */
    public Set<String> scenariosWithPendingChanges(String serviceId) {
        return changeRequestRepository.findAllByServiceIdOrderByCreatedAtDesc(UUID.fromString(serviceId)).stream()
                .filter(e -> e.getScenarioId() != null && !e.getScenarioId().isBlank())
                .filter(e -> !"approved".equals(e.getStatus()) && !"rejected".equals(e.getStatus()))
                .map(ChangeRequestEntity::getScenarioId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private ChangeRequest toDomain(ChangeRequestEntity e) {
        return new ChangeRequest(
                e.getId().toString(),
                e.getScenarioId(),
                e.getReason(),
                e.getTrigger(),
                e.getStatus(),
                e.getCreatedAt() == null ? null : e.getCreatedAt().toString(),
                e.getUpdatedAt() == null ? null : e.getUpdatedAt().toString(),
                e.getReviewedAt() == null ? null : e.getReviewedAt().toString(),
                e.getReviewer()
        );
    }
}
