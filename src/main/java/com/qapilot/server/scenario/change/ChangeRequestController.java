package com.qapilot.server.scenario.change;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.scenario.change.domain.ChangeRequest;
import com.qapilot.server.scenario.change.dto.UpdateChangeRequestRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시나리오 변경 요청 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}/scenario-change-requests")
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    public ChangeRequestController(ChangeRequestService changeRequestService) {
        this.changeRequestService = changeRequestService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @PathVariable String serviceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String trigger
    ) {
        List<ChangeRequest> requests = changeRequestService.list(serviceId, status, trigger);
        return ApiResponse.ok(Map.of("requests", requests, "count", requests.size()));
    }

    @PatchMapping("/{requestId}")
    public ApiResponse<Map<String, ChangeRequest>> update(
            @PathVariable String serviceId,
            @PathVariable String requestId,
            @RequestBody UpdateChangeRequestRequest request
    ) {
        return ApiResponse.ok(Map.of("request", changeRequestService.update(serviceId, requestId, request)));
    }
}
