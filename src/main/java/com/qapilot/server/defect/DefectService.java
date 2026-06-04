package com.qapilot.server.defect;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.defect.domain.Defect;
import com.qapilot.server.defect.dto.CreateDefectRequest;
import com.qapilot.server.defect.dto.UpdateDefectRequest;
import com.qapilot.server.defect.persistence.DefectEntity;
import com.qapilot.server.defect.persistence.DefectRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 결함 분석 결과 CRUD. Agent 자동 분류 흐름은 별도 PR 에서 추가.
 *
 * <p>Author: C
 * <br>Created: 2026-06-04
 */
@Service
public class DefectService {

    private static final Set<String> VALID_STATUS = Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED");

    private final DefectRepository defectRepository;

    public DefectService(DefectRepository defectRepository) {
        this.defectRepository = defectRepository;
    }

    public List<Defect> list(String serviceId, String status) {
        UUID svc = UUID.fromString(serviceId);
        List<DefectEntity> entities = status == null
                ? defectRepository.findAllByServiceIdOrderByCreatedAtDesc(svc)
                : defectRepository.findAllByServiceIdAndStatusOrderByCreatedAtDesc(svc, status);
        return entities.stream().map(this::toDomain).toList();
    }

    public Defect create(String serviceId, CreateDefectRequest request) {
        DefectEntity entity = new DefectEntity();
        entity.setId(UUID.randomUUID());
        entity.setServiceId(UUID.fromString(serviceId));
        entity.setRunId(UUID.fromString(request.runId()));
        entity.setTcResultId(request.tcResultId() == null ? null : UUID.fromString(request.tcResultId()));
        entity.setTsId(request.tsId());
        entity.setTcId(request.tcId());
        entity.setCategory(request.category());
        entity.setRootCauseTop1(request.rootCauseTop1());
        entity.setRootCauseConfidence(request.rootCauseConfidence());
        entity.setSolutionGuide(request.solutionGuide());
        entity.setAssignee(request.assignee());
        entity.setFileLocation(request.fileLocation());
        entity.setStatus("OPEN");
        defectRepository.save(entity);
        return toDomain(entity);
    }

    public Defect update(String serviceId, String defectId, UpdateDefectRequest request) {
        DefectEntity entity = requireEntity(serviceId, defectId);
        if (request.status() != null) {
            if (!VALID_STATUS.contains(request.status())) {
                throw new QapilotException(ErrorCode.DEFECT_002);
            }
            entity.setStatus(request.status());
        }
        if (request.assignee() != null) {
            entity.setAssignee(request.assignee());
        }
        if (request.solutionGuide() != null) {
            entity.setSolutionGuide(request.solutionGuide());
        }
        defectRepository.save(entity);
        return toDomain(entity);
    }

    public void delete(String serviceId, String defectId) {
        DefectEntity entity = requireEntity(serviceId, defectId);
        defectRepository.delete(entity);
    }

    private DefectEntity requireEntity(String serviceId, String defectId) {
        try {
            DefectEntity entity = defectRepository.findById(UUID.fromString(defectId))
                    .orElseThrow(() -> new QapilotException(ErrorCode.DEFECT_001));
            if (!entity.getServiceId().equals(UUID.fromString(serviceId))) {
                throw new QapilotException(ErrorCode.DEFECT_001);
            }
            return entity;
        } catch (IllegalArgumentException e) {
            throw new QapilotException(ErrorCode.DEFECT_001);
        }
    }

    private Defect toDomain(DefectEntity e) {
        return new Defect(
                e.getId().toString(),
                e.getServiceId().toString(),
                e.getRunId().toString(),
                e.getTcResultId() == null ? null : e.getTcResultId().toString(),
                e.getTsId(),
                e.getTcId(),
                e.getCategory(),
                e.getRootCauseTop1(),
                e.getRootCauseConfidence(),
                e.getSolutionGuide(),
                e.getAssignee(),
                e.getFileLocation(),
                e.getStatus(),
                e.getCreatedAt() == null ? null : e.getCreatedAt().toString(),
                e.getUpdatedAt() == null ? null : e.getUpdatedAt().toString()
        );
    }
}
