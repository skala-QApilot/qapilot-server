package com.qapilot.server.fastapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qapilot.server.common.config.QapilotProperties;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * FastAPI Agent 클라이언트 정책 테스트.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
class FastApiAgentClientTest {

    @Test
    void missingInternalTokenFailsAtCallTime() {
        QapilotProperties properties = new QapilotProperties(
                new QapilotProperties.Storage("."),
                new QapilotProperties.Fastapi("http://localhost:8001", "")
        );
        FastApiAgentClient client = new FastApiAgentClient(WebClient.builder(), properties);

        assertThatThrownBy(() -> client.startTestRun("service-id", "/tmp/.qapilot", List.of(), "all", null))
                .isInstanceOf(QapilotException.class)
                .satisfies(error -> assertThat(((QapilotException) error).errorCode()).isEqualTo(ErrorCode.AGENT_002));
    }
}
