package com.qapilot.server.rtm.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * RTM 버전 도메인.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RtmVersion(
        String rtmVersionId,
        String serviceId,
        String label,
        String traceId,
        List<RtmRequirement> requirements,
        RtmSummary summary,
        String createdAt
) {
}
