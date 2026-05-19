package com.qapilot.server.trace;

import com.qapilot.server.common.response.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * trace 상태 조회 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-19
 */
@RestController
@RequestMapping("/api/services/{serviceId}/traces")
public class TraceController {

    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping("/{traceId}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String serviceId, @PathVariable String traceId) {
        return ApiResponse.ok(Map.of("trace", traceService.get(serviceId, traceId)));
    }
}
