package com.qapilot.server.result;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.result.dto.ResultResponse;
import com.qapilot.server.result.dto.ResultStatisticsResponse;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import com.qapilot.server.trace.TraceFileStore;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * trace 기반 결과 조회 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class ResultQueryService {

    private final ServiceDomainService serviceDomainService;
    private final TraceFileStore traceFileStore;

    public ResultQueryService(ServiceDomainService serviceDomainService, TraceFileStore traceFileStore) {
        this.serviceDomainService = serviceDomainService;
        this.traceFileStore = traceFileStore;
    }

    public List<ResultResponse> list(String serviceId, String status, int limit, int offset) {
        return allResults(serviceId).stream()
                .filter(result -> status == null || status.isBlank() || status.equals(result.status()))
                .skip(Math.max(offset, 0))
                .limit(Math.max(limit, 1))
                .toList();
    }

    public int total(String serviceId, String status) {
        return (int) allResults(serviceId).stream()
                .filter(result -> status == null || status.isBlank() || status.equals(result.status()))
                .count();
    }

    public ResultResponse get(String serviceId, String traceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        Map<String, Object> trace = traceFileStore.findById(Path.of(service.qapilotDir()), traceId)
                .orElseThrow(() -> new QapilotException(ErrorCode.RESULT_001));
        if (!"test".equals(trace.get("command"))) {
            throw new QapilotException(ErrorCode.RESULT_001);
        }
        return ResultResponse.fromTrace(trace);
    }

    public ResultStatisticsResponse statistics(String serviceId) {
        List<ResultResponse> results = allResults(serviceId);
        int passed = (int) results.stream().filter(result -> "completed".equals(result.status())).count();
        int failed = (int) results.stream().filter(result -> "failed".equals(result.status())).count();
        Double passRate = results.isEmpty() ? null : (passed * 100.0) / results.size();
        return new ResultStatisticsResponse(results.size(), passed, failed, passRate);
    }

    public List<ResultResponse> allResults(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return traceFileStore.listAll(Path.of(service.qapilotDir())).stream()
                .filter(trace -> "test".equals(trace.get("command")))
                .map(ResultResponse::fromTrace)
                .toList();
    }
}
