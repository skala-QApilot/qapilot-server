package com.qapilot.server.rtm;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.rtm.domain.RtmRequirement;
import com.qapilot.server.rtm.domain.RtmVersion;
import com.qapilot.server.rtm.dto.CreateRtmVersionRequest;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RTM API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}/rtm-versions")
public class RtmController {

    private final RtmService rtmService;

    public RtmController(RtmService rtmService) {
        this.rtmService = rtmService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@PathVariable String serviceId) {
        List<RtmVersion> versions = rtmService.list(serviceId);
        return ApiResponse.ok(Map.of("versions", versions, "count", versions.size()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, RtmVersion>>> create(
            @PathVariable String serviceId,
            @RequestBody CreateRtmVersionRequest request
    ) {
        RtmVersion version = rtmService.create(serviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(Map.of("version", version)));
    }

    @GetMapping("/{rtmVersionId}")
    public ApiResponse<Map<String, RtmVersion>> get(
            @PathVariable String serviceId,
            @PathVariable String rtmVersionId
    ) {
        return ApiResponse.ok(Map.of("version", rtmService.get(serviceId, rtmVersionId)));
    }

    @GetMapping("/{rtmVersionId}/requirements")
    public ApiResponse<Map<String, Object>> requirements(
            @PathVariable String serviceId,
            @PathVariable String rtmVersionId
    ) {
        List<RtmRequirement> reqs = rtmService.getRequirements(serviceId, rtmVersionId);
        return ApiResponse.ok(Map.of("requirements", reqs, "count", reqs.size()));
    }

    @GetMapping("/{rtmVersionId}/requirements/{frId}")
    public ApiResponse<Map<String, RtmRequirement>> requirement(
            @PathVariable String serviceId,
            @PathVariable String rtmVersionId,
            @PathVariable String frId
    ) {
        return ApiResponse.ok(Map.of("requirement", rtmService.getRequirement(serviceId, rtmVersionId, frId)));
    }

    @GetMapping("/{rtmVersionId}/export")
    public ResponseEntity<String> export(
            @PathVariable String serviceId,
            @PathVariable String rtmVersionId
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rtm_" + rtmVersionId + ".json\"")
                .body(rtmService.export(serviceId, rtmVersionId));
    }
}
