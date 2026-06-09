package com.qapilot.server.service;

import com.qapilot.server.auth.security.AuthenticatedUser;
import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.service.dto.CredentialsResponse;
import com.qapilot.server.service.dto.ServiceCreateRequest;
import com.qapilot.server.service.dto.ServiceResponse;
import com.qapilot.server.service.dto.ServiceSetupRequest;
import com.qapilot.server.service.dto.ServiceUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;


/**
 * 서비스 도메인 API 컨트롤러.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceDomainService serviceDomainService;

    public ServiceController(ServiceDomainService serviceDomainService) {
        this.serviceDomainService = serviceDomainService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listServices() {
        List<ServiceResponse> services = serviceDomainService.listServices();
        return ApiResponse.ok(Map.of("services", services, "count", services.size()));
    }

    @PostMapping
    public ApiResponse<Map<String, ServiceResponse>> create(
            @Valid @RequestBody ServiceCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        UUID userId = UUID.fromString(user.userId());
        return ApiResponse.ok(Map.of("service", ServiceResponse.from(serviceDomainService.create(request, userId))));
    }

    @GetMapping("/{serviceId}")
    public ApiResponse<Map<String, ServiceResponse>> get(@PathVariable String serviceId) {
        return ApiResponse.ok(Map.of("service", ServiceResponse.from(serviceDomainService.getById(serviceId))));
    }

    @PatchMapping("/{serviceId}")
    public ApiResponse<Map<String, ServiceResponse>> update(
            @PathVariable String serviceId,
            @RequestBody ServiceUpdateRequest request
    ) {
        return ApiResponse.ok(Map.of("service", ServiceResponse.from(serviceDomainService.update(serviceId, request))));
    }

    @PostMapping("/{serviceId}/setup")
    public ApiResponse<Map<String, ServiceResponse>> setup(
            @PathVariable String serviceId,
            @RequestBody ServiceSetupRequest request
    ) {
        return ApiResponse.ok(Map.of("service", ServiceResponse.from(serviceDomainService.setup(serviceId, request))));
    }

    @GetMapping("/{serviceId}/credentials")
    public ApiResponse<Map<String, CredentialsResponse>> credentials(@PathVariable String serviceId) {
        return ApiResponse.ok(Map.of("credentials", serviceDomainService.credentials(serviceId)));
    }

    @PostMapping("/{serviceId}/credentials/token")
    public ApiResponse<Map<String, CredentialsResponse>> rotateToken(@PathVariable String serviceId) {
        return ApiResponse.ok(Map.of("credentials", CredentialsResponse.from(serviceDomainService.rotateToken(serviceId))));
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> delete(@PathVariable String serviceId) {
        serviceDomainService.delete(serviceId);
        return ResponseEntity.noContent().build();
    }
}
