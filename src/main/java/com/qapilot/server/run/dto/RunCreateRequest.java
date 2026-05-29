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
        List<String> tags,
        /** 이어서 실행: 지정 시 이전 trace 의 완료 TC 는 새 실행에서 스킵된다. */
        @JsonProperty("resume_from_trace") String resumeFromTrace
) {
}
