package com.qapilot.server.rtm.dto;

import com.qapilot.server.rtm.domain.RtmRequirement;
import java.util.List;

/**
 * RTM 버전 생성 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record CreateRtmVersionRequest(
        String label,
        String traceId,
        List<RtmRequirement> requirements
) {
}
