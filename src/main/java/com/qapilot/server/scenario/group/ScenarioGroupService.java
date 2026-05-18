package com.qapilot.server.scenario.group;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.scenario.group.domain.Schedule;
import com.qapilot.server.scenario.group.domain.ScenarioGroup;
import com.qapilot.server.scenario.group.dto.CreateScenarioGroupRequest;
import com.qapilot.server.scenario.group.dto.CreateScheduleRequest;
import com.qapilot.server.scenario.group.dto.UpdateScenarioGroupRequest;
import com.qapilot.server.service.ServiceDomainService;
import com.qapilot.server.service.domain.QapilotService;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 시나리오 그룹 관리 유스케이스.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Service
public class ScenarioGroupService {

    private final ServiceDomainService serviceDomainService;
    private final ScenarioGroupFileStore groupFileStore;

    public ScenarioGroupService(ServiceDomainService serviceDomainService, ScenarioGroupFileStore groupFileStore) {
        this.serviceDomainService = serviceDomainService;
        this.groupFileStore = groupFileStore;
    }

    public List<ScenarioGroup> list(String serviceId) {
        return groupFileStore.listAll(qapilotDir(serviceId));
    }

    public ScenarioGroup create(String serviceId, CreateScenarioGroupRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "name 필드가 필요합니다.");
        }
        String now = Instant.now().toString();
        ScenarioGroup group = new ScenarioGroup(
                UUID.randomUUID().toString(),
                request.name(),
                request.scenarioIds() == null ? List.of() : request.scenarioIds(),
                request.tcIds() == null ? List.of() : request.tcIds(),
                null,
                now,
                now
        );
        groupFileStore.save(qapilotDir(serviceId), group);
        return group;
    }

    public ScenarioGroup update(String serviceId, String groupId, UpdateScenarioGroupRequest request) {
        Path qapilotDir = qapilotDir(serviceId);
        ScenarioGroup current = groupFileStore.load(qapilotDir, groupId);
        ScenarioGroup updated = new ScenarioGroup(
                current.groupId(),
                request.name() != null ? request.name() : current.name(),
                current.scenarioIds(),
                current.tcIds(),
                current.schedule(),
                current.createdAt(),
                Instant.now().toString()
        );
        groupFileStore.save(qapilotDir, updated);
        return updated;
    }

    public void delete(String serviceId, String groupId) {
        groupFileStore.delete(qapilotDir(serviceId), groupId);
    }

    public ScenarioGroup setSchedule(String serviceId, String groupId, CreateScheduleRequest request) {
        if (request.cron() == null || request.cron().isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "cron 필드가 필요합니다.");
        }
        Path qapilotDir = qapilotDir(serviceId);
        ScenarioGroup current = groupFileStore.load(qapilotDir, groupId);
        Schedule schedule = new Schedule(
                request.cron(),
                request.timezone() == null ? "UTC" : request.timezone(),
                request.enabled() == null || request.enabled(),
                Instant.now().toString()
        );
        ScenarioGroup updated = new ScenarioGroup(
                current.groupId(),
                current.name(),
                current.scenarioIds(),
                current.tcIds(),
                schedule,
                current.createdAt(),
                Instant.now().toString()
        );
        groupFileStore.save(qapilotDir, updated);
        return updated;
    }

    private Path qapilotDir(String serviceId) {
        QapilotService service = serviceDomainService.getById(serviceId);
        return Path.of(service.qapilotDir());
    }
}
