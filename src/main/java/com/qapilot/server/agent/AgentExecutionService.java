package com.qapilot.server.agent;

import com.qapilot.server.agent.dto.AgentStartResponse;
import com.qapilot.server.agent.dto.CodeGenerationStartRequest;
import com.qapilot.server.agent.dto.ScenarioGenerationStartRequest;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.fastapi.FastApiAgentClient;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Spring Boot service-scope Agent 실행 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class AgentExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionService.class);

    private final ServiceDomainService serviceDomainService;
    private final FastApiAgentClient fastApiAgentClient;

    public AgentExecutionService(ServiceDomainService serviceDomainService, FastApiAgentClient fastApiAgentClient) {
        this.serviceDomainService = serviceDomainService;
        this.fastApiAgentClient = fastApiAgentClient;
    }

    public AgentStartResponse startScenarioGeneration(String serviceId, ScenarioGenerationStartRequest request) {
        validateScenarioGeneration(request);
        QapilotService service = serviceDomainService.getById(serviceId);
        String traceId = fastApiAgentClient.startScenarioGeneration(
                service.serviceId(),
                service.qapilotDir(),
                request.trigger(),
                request.userInput(),
                request.scenarioIds(),
                request.filter(),
                request.tags(),
                service.repos()
        );
        return AgentStartResponse.running(traceId);
    }

    public AgentStartResponse startCodeChangeDetection(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        String traceId = fastApiAgentClient.startCodeChangeDetection(
                service.serviceId(),
                service.qapilotDir(),
                service.repos()
        );
        return AgentStartResponse.running(traceId);
    }

    public AgentStartResponse startCodeGeneration(String serviceId, CodeGenerationStartRequest request) {
        return startCodeGeneration(
                serviceId,
                request == null ? null : request.scenarioIds(),
                request == null ? null : request.deletedTcIds(),
                request == null ? null : request.incremental()
        );
    }

    public AgentStartResponse startCodeGeneration(
            String serviceId,
            List<String> scenarioIds,
            List<String> deletedTcIds,
            Boolean incremental
    ) {
        QapilotService service = serviceDomainService.getById(serviceId);
        log.info(
                "start_code_generation serviceId={} qapilotDir={} scenarioIds={} deletedTcIds={} incremental={}",
                service.serviceId(),
                service.qapilotDir(),
                scenarioIds,
                deletedTcIds,
                incremental
        );
        String traceId = fastApiAgentClient.startCodeGeneration(
                service.serviceId(),
                service.qapilotDir(),
                scenarioIds,
                deletedTcIds,
                incremental
        );
        return AgentStartResponse.running(traceId);
    }

    private void validateScenarioGeneration(ScenarioGenerationStartRequest request) {
        if (request.trigger() == null || request.trigger().isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "trigger 필드가 필요합니다.");
        }
        if ("natural_lang".equals(request.trigger())
                && (request.userInput() == null || request.userInput().isBlank())) {
            throw new QapilotException(ErrorCode.COMMON_001, "natural_lang trigger는 user_input이 필요합니다.");
        }
    }
}
