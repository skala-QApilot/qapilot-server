package com.qapilot.server.rtm.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * RTM 요구사항 항목.
 *
 * <p>저장 시: frId / content / linkedTcIds 만 필수. status / passCount / totalCount / history 는
 * 응답 시점에 ScenarioStatusAggregator 결과 기반으로 RtmService 가 동적 계산해 채워준다.
 *
 * <p>같은 TC 가 여러 그룹에서 실행되어도 ScenarioStatusAggregator 가 최신 trace 의 status 1개만
 * 반환하므로 TC 단위 1회 카운트가 자연스럽게 보장된다.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RtmRequirement(
        String frId,
        String content,
        String status,
        int passCount,
        int totalCount,
        List<Map<String, Object>> history,
        List<String> linkedTcIds
) {
    /** 옛 trace JSON 호환용 (linkedTcIds 필드 없는 경우). */
    public RtmRequirement(String frId, String content, String status, int passCount, int totalCount,
                          List<Map<String, Object>> history) {
        this(frId, content, status, passCount, totalCount, history, List.of());
    }
}
