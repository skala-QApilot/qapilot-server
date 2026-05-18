package com.qapilot.server.service.dto;

/**
 * 프로젝트 URL 직접 진입용 대시보드 bootstrap 응답.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ProjectDashboardResponse(
        ServiceResponse project,
        ProjectSummaryResponse summary,
        CredentialsResponse credentials
) {
}
