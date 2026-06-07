package com.qapilot.server.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 서비스 범위 코드 생성 시작 요청.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
public record CodeGenerationStartRequest(
        @JsonProperty("scenario_ids") List<String> scenarioIds,
        @JsonProperty("deleted_tc_ids") List<String> deletedTcIds,
        @JsonProperty("incremental") Boolean incremental
) {
}
