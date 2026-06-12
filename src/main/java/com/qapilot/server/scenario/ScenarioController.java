package com.qapilot.server.scenario;

import com.qapilot.server.common.response.ApiResponse;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
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
 * 서비스 범위 시나리오 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}/scenarios")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @PathVariable String serviceId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String trigger
    ) {
        List<Map<String, Object>> scenarios = scenarioService.list(serviceId, search, trigger);
        return ApiResponse.ok(Map.of("scenarios", scenarios, "count", scenarios.size()));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(
            @PathVariable String serviceId,
            @RequestBody Map<String, Object> scenario
    ) {
        return ApiResponse.ok(Map.of("scenario", scenarioService.create(serviceId, scenario)));
    }

    @GetMapping("/{scenarioId}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String serviceId, @PathVariable String scenarioId) {
        return ApiResponse.ok(Map.of("scenario", scenarioService.get(serviceId, scenarioId)));
    }

    @PatchMapping("/{scenarioId}")
    public ApiResponse<Map<String, Object>> update(
            @PathVariable String serviceId,
            @PathVariable String scenarioId,
            @RequestBody Map<String, Object> patch
    ) {
        return ApiResponse.ok(Map.of("scenario", scenarioService.update(serviceId, scenarioId, patch)));
    }

    @DeleteMapping("/{scenarioId}")
    public ResponseEntity<Void> delete(@PathVariable String serviceId, @PathVariable String scenarioId) {
        scenarioService.delete(serviceId, scenarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{scenarioId}/test-cases")
    public ApiResponse<Map<String, Object>> testCases(
            @PathVariable String serviceId,
            @PathVariable String scenarioId
    ) {
        List<Map<String, Object>> testCases = scenarioService.testCases(serviceId, scenarioId);
        return ApiResponse.ok(Map.of("test_cases", testCases, "count", testCases.size()));
    }

    @GetMapping("/test-cases/{tcId}/action-mapping")
    public ApiResponse<Map<String, Object>> actionMapping(@PathVariable String serviceId, @PathVariable String tcId) {
        return ApiResponse.ok(Map.of("action_mapping", scenarioService.actionMapping(serviceId, tcId)));
    }

    @GetMapping("/documents/{filename}")
    public ResponseEntity<byte[]> document(@PathVariable String serviceId, @PathVariable String filename) {
        byte[] content = scenarioService.documentContent(serviceId, filename);
        String contentType = URLConnection.guessContentTypeFromName(filename);
        MediaType mediaType = contentType != null
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType).body(content);
    }

    @GetMapping("/source")
    public ApiResponse<Map<String, Object>> source(
            @PathVariable String serviceId,
            @RequestParam String file,
            @RequestParam String commitSha,
            @RequestParam(required = false) Integer lineStart,
            @RequestParam(required = false) Integer lineEnd
    ) {
        return ApiResponse.ok(scenarioService.sourceContent(serviceId, file, commitSha, lineStart, lineEnd));
    }
}
