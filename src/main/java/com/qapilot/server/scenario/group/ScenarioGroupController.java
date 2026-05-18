package com.qapilot.server.scenario.group;

import com.qapilot.server.common.response.ApiResponse;
import com.qapilot.server.scenario.group.domain.ScenarioGroup;
import com.qapilot.server.scenario.group.dto.CreateScenarioGroupRequest;
import com.qapilot.server.scenario.group.dto.CreateScheduleRequest;
import com.qapilot.server.scenario.group.dto.UpdateScenarioGroupRequest;
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
 * 시나리오 그룹 API.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestController
@RequestMapping("/api/services/{serviceId}/scenario-groups")
public class ScenarioGroupController {

    private final ScenarioGroupService groupService;

    public ScenarioGroupController(ScenarioGroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@PathVariable String serviceId) {
        List<ScenarioGroup> groups = groupService.list(serviceId);
        return ApiResponse.ok(Map.of("groups", groups, "count", groups.size()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, ScenarioGroup>>> create(
            @PathVariable String serviceId,
            @RequestBody CreateScenarioGroupRequest request
    ) {
        ScenarioGroup group = groupService.create(serviceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(Map.of("group", group)));
    }

    @PatchMapping("/{groupId}")
    public ApiResponse<Map<String, ScenarioGroup>> update(
            @PathVariable String serviceId,
            @PathVariable String groupId,
            @RequestBody UpdateScenarioGroupRequest request
    ) {
        return ApiResponse.ok(Map.of("group", groupService.update(serviceId, groupId, request)));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> delete(@PathVariable String serviceId, @PathVariable String groupId) {
        groupService.delete(serviceId, groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/schedule")
    public ApiResponse<Map<String, ScenarioGroup>> setSchedule(
            @PathVariable String serviceId,
            @PathVariable String groupId,
            @RequestBody CreateScheduleRequest request
    ) {
        return ApiResponse.ok(Map.of("group", groupService.setSchedule(serviceId, groupId, request)));
    }
}
