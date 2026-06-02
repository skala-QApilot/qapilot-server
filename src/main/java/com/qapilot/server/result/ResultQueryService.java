package com.qapilot.server.result;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.result.dto.ResultResponse;
import com.qapilot.server.result.dto.ResultStatisticsResponse;
import com.qapilot.server.run.RunReader;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * trace 기반 결과 조회 유스케이스.
 *
 * <p>PR-15c — file → DB read 전환. ResultResponse.fromTrace 가 보는 Map 구조는
 * RunReader 가 그대로 재현 (tc_results 는 tc_results 테이블에서 ui kind 만 추출).
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class ResultQueryService {

    private final ServiceDomainService serviceDomainService;
    private final RunReader runReader;

    public ResultQueryService(ServiceDomainService serviceDomainService, RunReader runReader) {
        this.serviceDomainService = serviceDomainService;
        this.runReader = runReader;
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
        Map<String, Object> trace = runReader.findById(traceId)
                .orElseThrow(() -> new QapilotException(ErrorCode.RESULT_001));
        if (!"test".equals(trace.get("command"))) {
            throw new QapilotException(ErrorCode.RESULT_001);
        }
        // service 존재만 확인하고 별 사용 없음 — 호환 유지를 위해 호출
        service.serviceId();
        return ResultResponse.fromTrace(trace);
    }

    public ResultStatisticsResponse statistics(String serviceId) {
        List<ResultResponse> results = allResults(serviceId);
        int passed = (int) results.stream().filter(result -> "completed".equals(result.status())).count();
        // "aborted": 파이프라인이 비정상 종료(예외/Ctrl+C 등). TC-level "failed" 와 다른 축.
        int failed = (int) results.stream().filter(result -> "aborted".equals(result.status())).count();
        Double passRate = results.isEmpty() ? null : (passed * 100.0) / results.size();
        return new ResultStatisticsResponse(results.size(), passed, failed, passRate);
    }

    public List<ResultResponse> allResults(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return runReader.listByServiceId(service.serviceId()).stream()
                .filter(trace -> "test".equals(trace.get("command")))
                .map(ResultResponse::fromTrace)
                .toList();
    }
}
