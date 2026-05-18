package com.qapilot.server.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FastAPI code-change-detection 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record CodeChangeDetectionRequest(
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("qapilot_dir") String qapilotDir
) {
}
