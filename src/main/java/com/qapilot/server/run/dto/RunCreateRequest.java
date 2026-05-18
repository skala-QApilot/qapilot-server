package com.qapilot.server.run.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 테스트 실행 생성 요청.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record RunCreateRequest(
        @JsonProperty("scenario_ids") List<String> scenarioIds,
        String filter,
        List<String> tags
) {
}
