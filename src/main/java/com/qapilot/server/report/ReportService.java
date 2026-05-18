package com.qapilot.server.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.report.store.ReportFileStore;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 리포트 조회 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class ReportService {

    private final ServiceDomainService serviceDomainService;
    private final ReportFileStore reportFileStore;
    private final ObjectMapper objectMapper;

    public ReportService(
            ServiceDomainService serviceDomainService,
            ReportFileStore reportFileStore,
            ObjectMapper objectMapper
    ) {
        this.serviceDomainService = serviceDomainService;
        this.reportFileStore = reportFileStore;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> list(String serviceId) {
        return reportFileStore.list(qapilotDir(serviceId));
    }

    public Map<String, Object> get(String serviceId, String traceId) {
        return reportFileStore.load(qapilotDir(serviceId), traceId);
    }

    public String export(String serviceId, String traceId) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(get(serviceId, traceId));
        } catch (JsonProcessingException e) {
            throw new QapilotException(ErrorCode.FILE_001, "리포트를 직렬화할 수 없습니다.");
        }
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
    }
}
