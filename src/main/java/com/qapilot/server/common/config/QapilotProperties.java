package com.qapilot.server.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * QApilot 서버 설정 프로퍼티.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@ConfigurationProperties(prefix = "qapilot")
public record QapilotProperties(Storage storage, Fastapi fastapi) {

    public QapilotProperties {
        storage = storage == null ? new Storage(".") : storage;
        fastapi = fastapi == null ? new Fastapi("http://localhost:8001", "") : fastapi;
    }

    public record Storage(String defaultTargetRoot) {
        public Storage {
            defaultTargetRoot = defaultTargetRoot == null || defaultTargetRoot.isBlank()
                    ? "."
                    : defaultTargetRoot;
        }
    }

    public record Fastapi(String baseUrl, String internalApiToken) {
        public Fastapi {
            baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8001" : baseUrl;
            internalApiToken = internalApiToken == null ? "" : internalApiToken;
        }

        public boolean hasInternalApiToken() {
            return !internalApiToken.isBlank();
        }
    }
}
