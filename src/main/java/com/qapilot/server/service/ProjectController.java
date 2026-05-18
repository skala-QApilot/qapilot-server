package com.qapilot.server.service;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.service.dto.ProjectDashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로젝트 URL 직접 진입용 API 컨트롤러.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ServiceDomainService serviceDomainService;

    public ProjectController(ServiceDomainService serviceDomainService) {
        this.serviceDomainService = serviceDomainService;
    }

    @GetMapping("/{projectSlug}")
    public ApiResponse<ProjectDashboardResponse> getProject(@PathVariable String projectSlug) {
        return ApiResponse.ok(serviceDomainService.projectDashboard(projectSlug));
    }
}
