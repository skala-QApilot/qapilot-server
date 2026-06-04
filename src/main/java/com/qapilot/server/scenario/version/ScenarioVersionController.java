package com.qapilot.server.scenario.version;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.scenario.version.domain.ScenarioVersion;
import com.qapilot.server.scenario.version.dto.CreateScenarioVersionRequest;
import com.qapilot.server.scenario.version.dto.UpdateScenarioVersionRequest;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 시나리오 버전 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}/scenario-versions")
public class ScenarioVersionController {

    private final ScenarioVersionService versionService;

    public ScenarioVersionController(ScenarioVersionService versionService) {
        this.versionService = versionService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@PathVariable String serviceId) {
        List<ScenarioVersion> versions = versionService.list(serviceId);
        return ApiResponse.ok(Map.of("versions", versions, "count", versions.size()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, ScenarioVersion>>> create(
            @PathVariable String serviceId,
            @RequestBody CreateScenarioVersionRequest request
    ) {
        ScenarioVersion version = versionService.create(serviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(Map.of("version", version)));
    }

    @PatchMapping("/{versionId}")
    public ApiResponse<Map<String, ScenarioVersion>> update(
            @PathVariable String serviceId,
            @PathVariable String versionId,
            @RequestBody UpdateScenarioVersionRequest request
    ) {
        return ApiResponse.ok(Map.of("version", versionService.update(serviceId, versionId, request)));
    }

    @DeleteMapping("/{versionId}")
    public ResponseEntity<Void> delete(@PathVariable String serviceId, @PathVariable String versionId) {
        versionService.delete(serviceId, versionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{versionId}/diff")
    public ApiResponse<Map<String, Object>> diff(
            @PathVariable String serviceId,
            @PathVariable String versionId
    ) {
        return ApiResponse.ok(Map.of("diff", versionService.diff(serviceId, versionId)));
    }

    /** 박힌 마일스톤의 시나리오들을 현재 작업 상태로 복원 (각 ts 의 새 version_number INSERT). */
    @PostMapping("/{versionId}/restore")
    public ApiResponse<Map<String, Object>> restore(
            @PathVariable String serviceId,
            @PathVariable String versionId
    ) {
        int restored = versionService.restore(serviceId, versionId);
        return ApiResponse.ok(Map.of("restoredCount", restored));
    }
}
