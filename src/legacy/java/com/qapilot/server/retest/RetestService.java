package com.qapilot.server.retest;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.retest.domain.RetestGroup;
import com.qapilot.server.retest.dto.CreateRetestGroupRequest;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 재테스트 그룹 관리 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class RetestService {

    private final ServiceDomainService serviceDomainService;
    private final RetestFileStore retestFileStore;

    public RetestService(ServiceDomainService serviceDomainService, RetestFileStore retestFileStore) {
        this.serviceDomainService = serviceDomainService;
        this.retestFileStore = retestFileStore;
    }

    public List<RetestGroup> list(String serviceId) {
        return retestFileStore.listAll(qapilotDir(serviceId));
    }

    public RetestGroup create(String serviceId, CreateRetestGroupRequest request) {
        if (request.sourceTraceId() == null || request.sourceTraceId().isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "sourceTraceId 필드가 필요합니다.");
        }
        if (request.failedTcIds() == null || request.failedTcIds().isEmpty()) {
            throw new QapilotException(ErrorCode.RETEST_002);
        }
        String date = LocalDate.now(ZoneOffset.UTC).toString();
        RetestGroup group = new RetestGroup(
                UUID.randomUUID().toString(),
                "재테스트 그룹 " + date,
                request.sourceTraceId(),
                request.failedTcIds(),
                "pending",
                Instant.now().toString()
        );
        retestFileStore.save(qapilotDir(serviceId), group);
        return group;
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
    }
}
