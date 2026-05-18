package com.qapilot.server.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * FastAPI 내부 호출 토큰 설정 상태를 기동 시점에 알린다.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class FastApiTokenStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FastApiTokenStartupValidator.class);

    private final QapilotProperties properties;

    public FastApiTokenStartupValidator(QapilotProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.fastapi().hasInternalApiToken()) {
            log.warn(
                    "QAPILOT_INTERNAL_API_TOKEN is not configured. "
                            + "FastAPI WebClient calls must fail until the token is provided."
            );
        }
    }
}
