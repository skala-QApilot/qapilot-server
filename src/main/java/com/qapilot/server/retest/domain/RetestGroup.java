package com.qapilot.server.retest.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 재테스트 그룹 도메인.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RetestGroup(
        String retestGroupId,
        String name,
        String sourceTraceId,
        List<String> failedTcIds,
        String status,
        String createdAt
) {
}
