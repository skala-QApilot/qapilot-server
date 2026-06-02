package com.qapilot.server.trace;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.run.RunReader;
import com.qapilot.server.service.ServiceDomainService;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * trace 상태 조회 유스케이스.
 *
 * <p>PR-15c — file 기반 TraceFileStore 에서 DB 기반 RunReader 로 read 경로 전환.
 * service 존재 여부는 ServiceDomainService 가, trace lookup 은 RunReader 가 담당.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
@Service
public class TraceService {

    private final ServiceDomainService serviceDomainService;
    private final RunReader runReader;

    public TraceService(ServiceDomainService serviceDomainService, RunReader runReader) {
        this.serviceDomainService = serviceDomainService;
        this.runReader = runReader;
    }

    public Map<String, Object> get(String serviceId, String traceId) {
        serviceDomainService.getById(serviceId);   // 존재/권한 체크
        return runReader.findById(traceId)
                .orElseThrow(() -> new QapilotException(ErrorCode.TRACE_001));
    }
}
