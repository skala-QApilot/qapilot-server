package com.qapilot.server.run;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.fastapi.FastApiAgentClient;
import com.qapilot.server.run.dto.RunCreateRequest;
import com.qapilot.server.run.dto.RunResponse;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import com.qapilot.server.trace.TraceFileStore;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 테스트 실행 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class RunService {

    private final ServiceDomainService serviceDomainService;
    private final TraceFileStore traceFileStore;
    private final FastApiAgentClient fastApiAgentClient;

    public RunService(
            ServiceDomainService serviceDomainService,
            TraceFileStore traceFileStore,
            FastApiAgentClient fastApiAgentClient
    ) {
        this.serviceDomainService = serviceDomainService;
        this.traceFileStore = traceFileStore;
        this.fastApiAgentClient = fastApiAgentClient;
    }

    public RunResponse start(String serviceId, RunCreateRequest request) {
        QapilotService service = serviceDomainService.getById(serviceId);
        String traceId = fastApiAgentClient.startTestRun(
                service.serviceId(),
                service.qapilotDir(),
                request.scenarioIds(),
                request.filter() == null || request.filter().isBlank() ? "all" : request.filter(),
                request.tags(),
                service.stagingUrl()
        );
        return RunResponse.running(traceId);
    }

    public List<RunResponse> active(String serviceId) {
        return traces(serviceId).stream()
                .filter(trace -> "running".equals(trace.get("status")))
                .map(RunResponse::fromTrace)
                .toList();
    }

    /** 모든 trace (running + completed) — 새로고침 후 이력 복원용. */
    public List<RunResponse> list(String serviceId) {
        return traces(serviceId).stream()
                .filter(trace -> "test".equals(trace.get("command")))
                .map(RunResponse::fromTrace)
                .toList();
    }

    public RunResponse get(String serviceId, String runId) {
        return RunResponse.fromTrace(traceWithPolling(serviceId, runId));
    }

    public Map<String, Object> progress(String serviceId, String runId) {
        Map<String, Object> trace = traceWithPolling(serviceId, runId);
        return Map.of(
                "stages", List.of(),
                "pass", 0,
                "fail", 0,
                "hitl_pending", 0,
                "status", trace.getOrDefault("status", "unknown")
        );
    }

    /** Layer 2 디스크 results 스캔 결과. UI 의 1초 폴링이 호출. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> runProgress(String serviceId, String runId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        Map<String, Object> response = fastApiAgentClient.runProgress(runId, service.qapilotDir());
        Object data = response == null ? null : response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            return (Map<String, Object>) dataMap;
        }
        return Map.of("trace_id", runId, "items", List.of(), "count", 0);
    }

    /** 가장 최근 PNG 스크린샷 bytes. 없으면 null. */
    public byte[] latestScreenshot(String serviceId, String runId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return fastApiAgentClient.latestScreenshot(runId, service.qapilotDir());
    }

    private Map<String, Object> trace(String serviceId, String traceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return traceFileStore.findById(Path.of(service.qapilotDir()), traceId)
                .orElseThrow(() -> new QapilotException(ErrorCode.RUN_001));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> traceWithPolling(String serviceId, String traceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        try {
            Map<String, Object> response = fastApiAgentClient.trace(traceId, service.qapilotDir());
            Object data = response == null ? null : response.get("data");
            if (data instanceof Map<?, ?> dataMap && dataMap.get("trace") instanceof Map<?, ?> trace) {
                return (Map<String, Object>) trace;
            }
            if (response != null && response.get("trace_id") != null) {
                return response;
            }
        } catch (QapilotException exception) {
            if (exception.errorCode() != ErrorCode.AGENT_001) {
                throw exception;
            }
            return trace(serviceId, traceId);
        }
        return trace(serviceId, traceId);
    }

    private List<Map<String, Object>> traces(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return traceFileStore.listAll(Path.of(service.qapilotDir()));
    }
}
