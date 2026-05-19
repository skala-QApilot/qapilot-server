package com.qapilot.server.agent;

import com.qapilot.server.agent.dto.AgentStartResponse;
import com.qapilot.server.agent.dto.CodeGenerationStartRequest;
import com.qapilot.server.agent.dto.ScenarioGenerationStartRequest;
import com.qapilot.server.common.response.ApiResponse;
import java.util.Map;
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
            @RequestBody(required = false) CodeGenerationStartRequest request
    ) {
        return ApiResponse.ok(Map.of("agent", agentExecutionService.startCodeGeneration(serviceId, request)));
    }
}
