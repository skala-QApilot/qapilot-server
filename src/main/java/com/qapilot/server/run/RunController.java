package com.qapilot.server.run;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.run.dto.RunCreateRequest;
import com.qapilot.server.run.dto.RunResponse;
import java.util.List;
import java.util.Map;
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

    @GetMapping("/{runId}")
    public ApiResponse<Map<String, RunResponse>> get(@PathVariable String serviceId, @PathVariable String runId) {
        return ApiResponse.ok(Map.of("run", runService.get(serviceId, runId)));
    }

    @GetMapping("/{runId}/agent-progress")
    public ApiResponse<Map<String, Object>> progress(@PathVariable String serviceId, @PathVariable String runId) {
        return ApiResponse.ok(runService.progress(serviceId, runId));
    }
}
