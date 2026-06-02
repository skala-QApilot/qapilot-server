package com.qapilot.server.organization;

import com.qapilot.server.auth.security.AuthenticatedUser;
import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.organization.dto.OrganizationResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Organization API — 현재 사용자가 속한 org 목록 조회.
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@RestController
@RequestMapping("/api/orgs")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/me")
    public ApiResponse<List<OrganizationResponse>> myOrgs(@AuthenticationPrincipal AuthenticatedUser user) {
        List<OrganizationResponse> orgs = organizationService.listForUser(UUID.fromString(user.userId())).stream()
                .map(OrganizationResponse::from)
                .toList();
        return ApiResponse.ok(orgs);
    }
}
