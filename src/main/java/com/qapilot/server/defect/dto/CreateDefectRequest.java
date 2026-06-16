package com.qapilot.server.defect.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Defect 생성 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-06-04
 */
public record CreateDefectRequest(
        @JsonProperty("run_id") String runId,
        @JsonProperty("tc_result_id") String tcResultId,
        @JsonProperty("ts_id") String tsId,
        @JsonProperty("tc_id") String tcId,
        String category,
        @JsonProperty("defect_type") String defectType,
        @JsonProperty("root_cause_top1") String rootCauseTop1,
        @JsonProperty("root_cause_confidence") BigDecimal rootCauseConfidence,
        @JsonProperty("solution_guide") String solutionGuide,
        String assignee,
        @JsonProperty("file_location") String fileLocation
) {}
