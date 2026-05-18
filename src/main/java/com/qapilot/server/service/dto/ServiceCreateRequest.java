package com.qapilot.server.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 서비스 생성 요청.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ServiceCreateRequest(
        String name,
        String description,
        @JsonProperty("target_root") String targetRoot
) {
}
