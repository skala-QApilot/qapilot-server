package com.qapilot.server.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qapilot.server.service.domain.RepoConfig;
import java.util.List;

/**
 * FastAPI code-change-detection 요청 DTO.
 *
 * <p>repos 가 채워져 있으면 FastAPI 가 GitCodebaseScannerTool 로 원격 git diff
 * 기반 변경 감지를 수행. null 이면 로컬 git 디렉토리 스캔.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CodeChangeDetectionRequest(
        @JsonProperty("service_id") String serviceId,
        List<RepoConfig> repos
) {
}
