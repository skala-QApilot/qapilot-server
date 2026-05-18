package com.qapilot.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * QApilot Spring Boot 중앙 API 서버 애플리케이션.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class QapilotServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(QapilotServerApplication.class, args);
    }
}
