package com.qapilot.server.retest.dto;

import java.util.List;

/**
 * 재테스트 그룹 생성 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record CreateRetestGroupRequest(String sourceTraceId, List<String> failedTcIds) {
}
