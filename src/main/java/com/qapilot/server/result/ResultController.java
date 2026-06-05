package com.qapilot.server.result;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.result.dto.ResultResponse;
import com.qapilot.server.result.dto.ResultStatisticsResponse;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    /** TC 한 건의 ui_result.json 본문 — tc_results.payload (JSONB) 반환. */
    @GetMapping("/{traceId}/tc-result")
    public ApiResponse<Map<String, Object>> getTcResult(
            @PathVariable String serviceId,
            @PathVariable String traceId,
            @RequestParam(name = "ts_id") String tsId,
            @RequestParam(name = "tc_id") String tcId
    ) {
        Map<String, Object> ui = resultQueryService.getUiResult(serviceId, traceId, tsId, tcId);
        return ApiResponse.ok(Map.of("ui_result", ui));
    }

    /** TC 의 에러 시점 스크린샷 — S3 에서 byte 로 받아 image/png 응답. step 기본 1. */
    @GetMapping("/{traceId}/tc-screenshot")
    public ResponseEntity<byte[]> getTcScreenshot(
            @PathVariable String serviceId,
            @PathVariable String traceId,
            @RequestParam(name = "ts_id") String tsId,
            @RequestParam(name = "tc_id") String tcId,
            @RequestParam(name = "step", defaultValue = "1") int step
    ) {
        byte[] data = resultQueryService.getScreenshot(serviceId, traceId, tsId, tcId, step);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("private, max-age=3600");
        return new ResponseEntity<>(data, headers, org.springframework.http.HttpStatus.OK);
    }
}
