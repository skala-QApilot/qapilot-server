package com.qapilot.server.rtm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.rtm.domain.RtmRequirement;
import com.qapilot.server.rtm.domain.RtmSummary;
import com.qapilot.server.rtm.domain.RtmVersion;
import com.qapilot.server.rtm.dto.CreateRtmVersionRequest;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * RTM 관리 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class RtmService {

    private final ServiceDomainService serviceDomainService;
    private final RtmFileStore rtmFileStore;
    private final ObjectMapper objectMapper;

    public RtmService(ServiceDomainService serviceDomainService, RtmFileStore rtmFileStore, ObjectMapper objectMapper) {
        this.serviceDomainService = serviceDomainService;
        this.rtmFileStore = rtmFileStore;
        this.objectMapper = objectMapper;
    }

    public List<RtmVersion> list(String serviceId) {
        return rtmFileStore.listAll(qapilotDir(serviceId));
    }

    public RtmVersion create(String serviceId, CreateRtmVersionRequest request) {
        if (request.label() == null || request.label().isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "label 필드가 필요합니다.");
        }
        List<RtmRequirement> requirements = request.requirements() == null ? List.of() : request.requirements();
        RtmSummary summary = RtmSummary.from(requirements);
        RtmVersion version = new RtmVersion(
                UUID.randomUUID().toString(),
                serviceId,
                request.label(),
                request.traceId(),
                requirements,
                summary,
                Instant.now().toString()
        );
        rtmFileStore.save(qapilotDir(serviceId), version);
        return version;
    }

    public RtmVersion get(String serviceId, String rtmVersionId) {
        return rtmFileStore.load(qapilotDir(serviceId), rtmVersionId);
    }

    public List<RtmRequirement> getRequirements(String serviceId, String rtmVersionId) {
        return rtmFileStore.load(qapilotDir(serviceId), rtmVersionId).requirements();
    }

    public RtmRequirement getRequirement(String serviceId, String rtmVersionId, String frId) {
        return getRequirements(serviceId, rtmVersionId).stream()
                .filter(req -> frId.equals(req.frId()))
                .findFirst()
                .orElseThrow(() -> new QapilotException(ErrorCode.RTM_002));
    }

    public String export(String serviceId, String rtmVersionId) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(get(serviceId, rtmVersionId));
        } catch (JsonProcessingException e) {
            throw new QapilotException(ErrorCode.FILE_001, "RTM을 직렬화할 수 없습니다.");
        }
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
    }
}
