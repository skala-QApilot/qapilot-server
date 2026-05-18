package com.qapilot.server.report;

import com.qapilot.server.common.response.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 리포트 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@PathVariable String serviceId) {
        List<Map<String, Object>> reports = reportService.list(serviceId);
        return ApiResponse.ok(Map.of("reports", reports, "count", reports.size()));
    }

    @GetMapping("/{traceId}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String serviceId, @PathVariable String traceId) {
        return ApiResponse.ok(Map.of("report", reportService.get(serviceId, traceId)));
    }

    @GetMapping("/{traceId}/export")
    public ResponseEntity<String> export(@PathVariable String serviceId, @PathVariable String traceId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report_" + traceId + ".json\"")
                .body(reportService.export(serviceId, traceId));
    }
}
