package com.qapilot.server.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 프로젝트 코드베이스 요약 응답.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ProjectSummaryResponse(
        @JsonProperty("file_count") int fileCount,
        @JsonProperty("endpoint_count") int endpointCount,
        @JsonProperty("model_count") int modelCount,
        @JsonProperty("last_scanned_at") String lastScannedAt,
        @JsonProperty("has_index") boolean hasIndex,
        List<EndpointSummary> endpoints
) {
    public record EndpointSummary(String path, String method, String handler, String file) {
    }
}
