package com.qapilot.server.graph;

import com.qapilot.server.common.response.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시나리오 그래프/플로우 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/scenario-graph")
    public ApiResponse<Map<String, Object>> graph(@PathVariable String serviceId) {
        return ApiResponse.ok(graphService.graph(serviceId));
    }

    @GetMapping("/scenario-flow")
    public ApiResponse<Map<String, Object>> flow(@PathVariable String serviceId) {
        return ApiResponse.ok(graphService.flow(serviceId));
    }
}
