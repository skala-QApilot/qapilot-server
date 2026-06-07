package com.qapilot.server.agent;

import com.qapilot.server.agent.dto.AgentStartResponse;
import com.qapilot.server.agent.dto.ScenarioGenerationStartRequest;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.response.ApiResponse;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 범위 Agent 실행 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}")
public class AgentExecutionController {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionController.class);

    private final AgentExecutionService agentExecutionService;

    public AgentExecutionController(AgentExecutionService agentExecutionService) {
        this.agentExecutionService = agentExecutionService;
    }

    @PostMapping("/scenario-generation")
    public ApiResponse<Map<String, AgentStartResponse>> scenarioGeneration(
            @PathVariable String serviceId,
            @RequestBody ScenarioGenerationStartRequest request
    ) {
        return ApiResponse.ok(Map.of("agent", agentExecutionService.startScenarioGeneration(serviceId, request)));
    }

    @PostMapping("/code-change-detection")
    public ApiResponse<Map<String, AgentStartResponse>> codeChangeDetection(@PathVariable String serviceId) {
        return ApiResponse.ok(Map.of("agent", agentExecutionService.startCodeChangeDetection(serviceId)));
    }

    @PostMapping("/code-generation")
    public ApiResponse<Map<String, AgentStartResponse>> codeGeneration(
            @PathVariable String serviceId,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        List<String> scenarioIds = optionalStringList(request, "scenario_ids");
        List<String> deletedTcIds = optionalStringList(request, "deleted_tc_ids");
        Boolean incremental = optionalBoolean(request, "incremental");
        log.info(
                "code_generation_request serviceId={} scenarioIds={} deletedTcIds={} incremental={}",
                serviceId,
                scenarioIds,
                deletedTcIds,
                incremental
        );
        try {
            return ApiResponse.ok(Map.of("agent", agentExecutionService.startCodeGeneration(
                    serviceId,
                    scenarioIds,
                    deletedTcIds,
                    incremental
            )));
        } catch (QapilotException e) {
            throw e;
        } catch (Exception e) {
            log.error("code_generation_request_failed serviceId={}", serviceId, e);
            throw new QapilotException(
                    ErrorCode.AGENT_001,
                    "코드 생성 트리거 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage()
            );
        }
    }

    private List<String> optionalStringList(Map<String, Object> body, String key) {
        if (body == null || !(body.get(key) instanceof List<?> list)) {
            return null;
        }
        return list.stream()
                .filter(item -> item != null)
                .map(String::valueOf)
                .toList();
    }

    private Boolean optionalBoolean(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        Object value = body.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.valueOf(String.valueOf(value));
    }
}
