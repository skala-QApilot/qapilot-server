package com.qapilot.server.cli;

import com.qapilot.server.cli.dto.CliSyncRequest;
import com.qapilot.server.cli.dto.CliSyncResult;
import com.qapilot.server.cli.dto.GeneratedCodeSyncItem;
import com.qapilot.server.cli.dto.ResultSyncItem;
import com.qapilot.server.cli.security.CliServiceTokenResolver;
import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.service.domain.QapilotService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CLI sync API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/cli")
public class CliSyncController {

    private final CliServiceTokenResolver tokenResolver;
    private final CliSyncService cliSyncService;

    public CliSyncController(CliServiceTokenResolver tokenResolver, CliSyncService cliSyncService) {
        this.tokenResolver = tokenResolver;
        this.cliSyncService = cliSyncService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "version", "0.1.0");
    }

    @PostMapping("/sync/scenarios")
    public ApiResponse<CliSyncResult> syncScenarios(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CliSyncRequest<Map<String, Object>> request
    ) {
        QapilotService service = tokenResolver.resolve(authorization);
        return ApiResponse.ok(cliSyncService.syncScenarios(service, request));
    }

    @PostMapping("/sync/generated-code")
    public ApiResponse<CliSyncResult> syncGeneratedCode(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CliSyncRequest<GeneratedCodeSyncItem> request
    ) {
        QapilotService service = tokenResolver.resolve(authorization);
        return ApiResponse.ok(cliSyncService.syncGeneratedCode(service, request));
    }

    @PostMapping("/sync/results")
    public ApiResponse<CliSyncResult> syncResults(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CliSyncRequest<ResultSyncItem> request
    ) {
        QapilotService service = tokenResolver.resolve(authorization);
        return ApiResponse.ok(cliSyncService.syncResults(service, request));
    }
}
