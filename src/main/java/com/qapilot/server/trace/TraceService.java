package com.qapilot.server.trace;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * trace 상태 조회 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
@Service
public class TraceService {

    private final ServiceDomainService serviceDomainService;
    private final TraceFileStore traceFileStore;

    public TraceService(ServiceDomainService serviceDomainService, TraceFileStore traceFileStore) {
        this.serviceDomainService = serviceDomainService;
        this.traceFileStore = traceFileStore;
    }

    public Map<String, Object> get(String serviceId, String traceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return traceFileStore.findById(Path.of(service.qapilotDir()), traceId)
                .orElseThrow(() -> new QapilotException(ErrorCode.TRACE_001));
    }
}
