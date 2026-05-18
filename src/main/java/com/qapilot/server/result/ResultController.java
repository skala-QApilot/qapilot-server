package com.qapilot.server.result;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.result.dto.ResultResponse;
import com.qapilot.server.result.dto.ResultStatisticsResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트 결과 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}/results")
public class ResultController {

    private final ResultQueryService resultQueryService;

    public ResultController(ResultQueryService resultQueryService) {
        this.resultQueryService = resultQueryService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @PathVariable String serviceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        List<ResultResponse> results = resultQueryService.list(serviceId, status, limit, offset);
        int total = resultQueryService.total(serviceId, status);
        return ApiResponse.ok(Map.of("results", results, "count", results.size(), "total", total));
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, ResultStatisticsResponse>> statistics(@PathVariable String serviceId) {
        return ApiResponse.ok(Map.of("statistics", resultQueryService.statistics(serviceId)));
    }

    @GetMapping("/{traceId}")
    public ApiResponse<Map<String, ResultResponse>> get(@PathVariable String serviceId, @PathVariable String traceId) {
        return ApiResponse.ok(Map.of("result", resultQueryService.get(serviceId, traceId)));
    }
}
