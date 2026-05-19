package com.qapilot.server.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * QApilot 서버 설정 프로퍼티.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@ConfigurationProperties(prefix = "qapilot")
public record QapilotProperties(Storage storage, Fastapi fastapi, Cors cors) {

    public QapilotProperties {
        storage = storage == null ? new Storage(".") : storage;
        fastapi = fastapi == null ? new Fastapi("http://localhost:8001", "") : fastapi;
        cors = cors == null ? new Cors(null, null, null, null) : cors;
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

    public record Cors(
            List<String> allowedOrigins,
            List<String> allowedMethods,
            List<String> allowedHeaders,
            Boolean allowCredentials
    ) {
        public Cors {
            allowedOrigins = (allowedOrigins == null || allowedOrigins.isEmpty())
                    ? List.of("http://localhost:5173")
                    : List.copyOf(allowedOrigins);
            allowedMethods = (allowedMethods == null || allowedMethods.isEmpty())
                    ? List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    : List.copyOf(allowedMethods);
            allowedHeaders = (allowedHeaders == null || allowedHeaders.isEmpty())
                    ? List.of("*")
                    : List.copyOf(allowedHeaders);
            allowCredentials = allowCredentials == null ? Boolean.TRUE : allowCredentials;
        }
    }
}
