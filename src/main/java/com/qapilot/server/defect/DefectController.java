package com.qapilot.server.defect;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.defect.domain.Defect;
import com.qapilot.server.defect.dto.CreateDefectRequest;
import com.qapilot.server.defect.dto.UpdateDefectRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결함(defect) API.
 *
 * <p>Author: C
 * <br>Created: 2026-06-04
 */
@RestController
@RequestMapping("/api/services/{serviceId}/defects")
public class DefectController {

    private final DefectService defectService;

    public DefectController(DefectService defectService) {
        this.defectService = defectService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @PathVariable String serviceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "run_id") String runId
    ) {
        List<Defect> defects = runId != null
                ? defectService.listByRun(serviceId, runId)
                : defectService.list(serviceId, status);
        return ApiResponse.ok(Map.of("defects", defects, "count", defects.size()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Defect>>> create(
            @PathVariable String serviceId,
            @RequestBody CreateDefectRequest request
    ) {
        Defect defect = defectService.create(serviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(Map.of("defect", defect)));
    }

    @PatchMapping("/{defectId}")
    public ApiResponse<Map<String, Defect>> update(
            @PathVariable String serviceId,
            @PathVariable String defectId,
            @RequestBody UpdateDefectRequest request
    ) {
        return ApiResponse.ok(Map.of("defect", defectService.update(serviceId, defectId, request)));
    }

    @DeleteMapping("/{defectId}")
    public ResponseEntity<Void> delete(
            @PathVariable String serviceId,
            @PathVariable String defectId
    ) {
        defectService.delete(serviceId, defectId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{defectId}/github-issue")
    public ApiResponse<Map<String, Defect>> createGithubIssue(
            @PathVariable String serviceId,
            @PathVariable String defectId
    ) {
        return ApiResponse.ok(Map.of("defect", defectService.createGithubIssue(serviceId, defectId)));
    }
}
