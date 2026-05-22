package com.qapilot.server.fastapi;

import com.qapilot.server.common.config.QapilotProperties;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.fastapi.dto.AgentRunRequest;
import com.qapilot.server.fastapi.dto.CodeChangeDetectionRequest;
import com.qapilot.server.fastapi.dto.CodeGenerationRequest;
import com.qapilot.server.fastapi.dto.ScenarioGenerationRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * FastAPI Agent 실행 서버 연동 클라이언트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class FastApiAgentClient {

    private final WebClient webClient;
    private final QapilotProperties properties;

    public FastApiAgentClient(WebClient.Builder builder, QapilotProperties properties) {
        this.properties = properties;
        this.webClient = builder.baseUrl(properties.fastapi().baseUrl()).build();
    }

    public String startTestRun(
            String serviceId,
            String qapilotDir,
            List<String> scenarioIds,
            String filter,
            List<String> tags
    ) {
        AgentRunRequest request = new AgentRunRequest(serviceId, qapilotDir, scenarioIds, filter, tags);
        Map<String, Object> response = post("/api/agent/test-run", request);
        return extractTraceId(response);
    }

    public String startScenarioGeneration(
            String serviceId,
            String qapilotDir,
            String trigger,
            String userInput,
            List<String> scenarioIds,
            String filter,
            List<String> tags,
            String repoUrl,
            String repoToken,
            String repoBranch
    ) {
        ScenarioGenerationRequest request = new ScenarioGenerationRequest(
                serviceId, qapilotDir, trigger, userInput, scenarioIds, filter, tags,
                repoUrl, repoToken, repoBranch
        );
        return extractTraceId(post("/api/agent/scenario-generation", request));
    }

    public String startCodeChangeDetection(
            String serviceId,
            String qapilotDir,
            String repoUrl,
            String repoToken,
            String repoBranch
    ) {
        CodeChangeDetectionRequest request = new CodeChangeDetectionRequest(
                serviceId, qapilotDir, repoUrl, repoToken, repoBranch
        );
        return extractTraceId(post("/api/agent/code-change-detection", request));
    }

    public String startCodeGeneration(String serviceId, String qapilotDir, List<String> scenarioIds) {
        CodeGenerationRequest request = new CodeGenerationRequest(serviceId, qapilotDir, scenarioIds);
        return extractTraceId(post("/api/agent/code-generation", request));
    }

    public Map<String, Object> trace(String traceId, String qapilotDir) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/agent/traces/{traceId}")
                            .queryParam("qapilot_dir", qapilotDir)
                            .build(traceId))
                    .headers(this::internalHeaders)
                    .retrieve()
                    .bodyToMono(mapType())
                    .block();
        } catch (Exception e) {
            throw mapAgentException(e, "FastAPI trace 조회에 실패했습니다.");
        }
    }

    private Map<String, Object> post(String uri, Object body) {
        try {
            return webClient.post()
                    .uri(uri)
                    .headers(this::internalHeaders)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(mapType())
                    .block();
        } catch (Exception e) {
            throw mapAgentException(e, "FastAPI Agent 호출에 실패했습니다.");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTraceId(Map<String, Object> response) {
        if (response == null) {
            throw new QapilotException(ErrorCode.AGENT_001);
        }
        Object traceId = response.get("trace_id");
        if (traceId != null) {
            return String.valueOf(traceId);
        }
        Object data = response.get("data");
        if (data instanceof Map<?, ?> map && map.get("trace_id") != null) {
            return String.valueOf(map.get("trace_id"));
        }
        throw new QapilotException(ErrorCode.AGENT_001, "FastAPI 응답에 trace_id가 없습니다.");
    }

    private void internalHeaders(HttpHeaders headers) {
        if (!properties.fastapi().hasInternalApiToken()) {
            throw new QapilotException(ErrorCode.AGENT_002, "QAPILOT_INTERNAL_API_TOKEN이 설정되지 않았습니다.");
        }
        headers.setBearerAuth(properties.fastapi().internalApiToken());
    }

    private QapilotException mapAgentException(Exception exception, String fallbackMessage) {
        if (exception instanceof QapilotException qapilotException) {
            return qapilotException;
        }
        if (exception instanceof WebClientResponseException responseException) {
            HttpStatus status = HttpStatus.valueOf(responseException.getStatusCode().value());
            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                return new QapilotException(ErrorCode.AGENT_002, "FastAPI 내부 인증에 실패했습니다.");
            }
            if (status.is4xxClientError()) {
                return new QapilotException(ErrorCode.AGENT_003, "FastAPI Agent 요청이 올바르지 않습니다.");
            }
            return new QapilotException(ErrorCode.AGENT_001, fallbackMessage);
        }
        return new QapilotException(ErrorCode.AGENT_001, fallbackMessage);
    }

    @SuppressWarnings("unchecked")
    private Class<Map<String, Object>> mapType() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }
}
