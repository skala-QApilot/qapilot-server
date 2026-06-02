package com.qapilot.server.evidence;

import com.qapilot.server.common.response.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 증적 조회 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}/evidences")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping("/{traceId}")
    public ApiResponse<Map<String, Object>> index(@PathVariable String serviceId, @PathVariable String traceId) {
        return ApiResponse.ok(Map.of("evidence", evidenceService.index(serviceId, traceId)));
    }

    @GetMapping("/{traceId}/{tcId}")
    public ApiResponse<Map<String, Object>> tcEvidence(
            @PathVariable String serviceId,
            @PathVariable String traceId,
            @PathVariable String tcId
    ) {
        return ApiResponse.ok(Map.of("evidence", evidenceService.tcEvidence(serviceId, traceId, tcId)));
    }
}
