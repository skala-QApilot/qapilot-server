package com.qapilot.server.defect.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Defect API 응답 record.
 *
 * <p>Author: C
 * <br>Created: 2026-06-04
 */
public record Defect(
        String id,
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("run_id") String runId,
        @JsonProperty("tc_result_id") String tcResultId,
        @JsonProperty("ts_id") String tsId,
        @JsonProperty("tc_id") String tcId,
        String category,
        @JsonProperty("root_cause_top1") String rootCauseTop1,
        @JsonProperty("root_cause_confidence") BigDecimal rootCauseConfidence,
        @JsonProperty("solution_guide") String solutionGuide,
        String assignee,
        @JsonProperty("file_location") String fileLocation,
        @JsonProperty("issue_url") String issueUrl,
        String status,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt
) {}
