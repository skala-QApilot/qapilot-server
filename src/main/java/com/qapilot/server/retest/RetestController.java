package com.qapilot.server.retest;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.retest.domain.RetestGroup;
import com.qapilot.server.retest.dto.CreateRetestGroupRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 재테스트 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}/retest-groups")
public class RetestController {

    private final RetestService retestService;

    public RetestController(RetestService retestService) {
        this.retestService = retestService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@PathVariable String serviceId) {
        List<RetestGroup> groups = retestService.list(serviceId);
        return ApiResponse.ok(Map.of("retestGroups", groups, "count", groups.size()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, RetestGroup>>> create(
            @PathVariable String serviceId,
            @RequestBody CreateRetestGroupRequest request
    ) {
        RetestGroup group = retestService.create(serviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(Map.of("retestGroup", group)));
    }
}
