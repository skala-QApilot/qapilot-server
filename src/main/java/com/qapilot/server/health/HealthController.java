package com.qapilot.server.health;

import com.qapilot.server.common.response.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 상태 확인 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "ok", "version", "0.1.0"));
    }
}
