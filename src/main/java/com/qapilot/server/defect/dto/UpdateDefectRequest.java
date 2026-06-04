package com.qapilot.server.defect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defect 상태/담당자 등 메타 갱신용 DTO. 본문(category, root_cause_*) 은 immutable.
 *
 * <p>Author: C
 * <br>Created: 2026-06-04
 */
public record UpdateDefectRequest(
        String status,
        String assignee,
        @JsonProperty("solution_guide") String solutionGuide
) {}
