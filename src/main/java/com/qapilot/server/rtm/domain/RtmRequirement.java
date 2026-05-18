package com.qapilot.server.rtm.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * RTM 요구사항 항목.
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
        List<Map<String, Object>> history
) {
}
