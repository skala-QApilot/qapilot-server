package com.qapilot.server.cli.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * 테스트 결과 sync 항목.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ResultSyncItem(@JsonProperty("trace_id") String traceId, Map<String, Object> data) {
}
