package com.qapilot.server.run;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.run.dto.RunCreateRequest;
import com.qapilot.server.run.dto.RunResponse;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트 실행 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}/runs")
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> start(
            @PathVariable String serviceId,
            @RequestBody RunCreateRequest request
    ) {
        RunResponse run = runService.start(serviceId, request);
        return ApiResponse.ok(Map.of("run_id", run.id(), "status", run.status()));
    }

    @GetMapping("/active")
    public ApiResponse<Map<String, Object>> active(@PathVariable String serviceId) {
        List<RunResponse> runs = runService.active(serviceId);
        return ApiResponse.ok(Map.of("runs", runs, "count", runs.size()));
    }

    /** 새로고침 후 이력 복원용 — running + completed 모든 trace 반환. */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(@PathVariable String serviceId) {
        List<RunResponse> runs = runService.list(serviceId);
        return ApiResponse.ok(Map.of("runs", runs, "count", runs.size()));
    }

    @GetMapping("/{runId}")
    public ApiResponse<Map<String, RunResponse>> get(@PathVariable String serviceId, @PathVariable String runId) {
        return ApiResponse.ok(Map.of("run", runService.get(serviceId, runId)));
    }

    @GetMapping("/{runId}/agent-progress")
    public ApiResponse<Map<String, Object>> progress(@PathVariable String serviceId, @PathVariable String runId) {
        return ApiResponse.ok(runService.progress(serviceId, runId));
    }

    /** Layer 2 진행 상황 — TC별 ui/api/db 결과. UI 의 1초 폴링이 호출. */
    @GetMapping("/{runId}/run-progress")
    public ApiResponse<Map<String, Object>> runProgress(@PathVariable String serviceId, @PathVariable String runId) {
        return ApiResponse.ok(runService.runProgress(serviceId, runId));
    }

    /** 가장 최근 PNG 스크린샷. UI 의 1초 폴링이 호출. 없으면 204. */
    @GetMapping("/{runId}/screenshot/latest")
    public ResponseEntity<byte[]> latestScreenshot(@PathVariable String serviceId, @PathVariable String runId) {
        byte[] png = runService.latestScreenshot(serviceId, runId);
        if (png == null || png.length == 0) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }
}
