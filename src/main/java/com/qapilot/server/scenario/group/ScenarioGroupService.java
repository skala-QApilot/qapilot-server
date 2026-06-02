package com.qapilot.server.scenario.group;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.scenario.group.domain.Schedule;
import com.qapilot.server.scenario.group.domain.ScenarioGroup;
import com.qapilot.server.scenario.group.dto.CreateScenarioGroupRequest;
import com.qapilot.server.scenario.group.dto.CreateScheduleRequest;
import com.qapilot.server.scenario.group.dto.UpdateScenarioGroupRequest;
import com.qapilot.server.scenario.group.persistence.ScenarioGroupEntity;
import com.qapilot.server.scenario.group.persistence.ScenarioGroupRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 시나리오 그룹 관리. PR-16 — JPA only.
 *
 * <p>scenarioIds / tcIds 는 scenario_group_members 같은 별 테이블 대신 inline JSONB 컬럼 사용.
 * schedule (cron/timezone/enabled/createdAt) 도 inline 컬럼.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18, rewritten 2026-06-02
 */
@Service
public class ScenarioGroupService {

    private static final TypeReference<List<String>> STR_LIST = new TypeReference<>() {};

    private final ScenarioGroupRepository groupRepository;
    private final ObjectMapper objectMapper;

    public ScenarioGroupService(ScenarioGroupRepository groupRepository, ObjectMapper objectMapper) {
        this.groupRepository = groupRepository;
        this.objectMapper = objectMapper;
    }

    public List<ScenarioGroup> list(String serviceId) {
        return groupRepository.findAllByServiceIdOrderByCreatedAtDesc(UUID.fromString(serviceId)).stream()
                .map(this::toDomain)
                .toList();
    }

    public ScenarioGroup create(String serviceId, CreateScenarioGroupRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "name 필드가 필요합니다.");
        }
        ScenarioGroupEntity entity = new ScenarioGroupEntity();
        entity.setId(UUID.randomUUID());
        entity.setServiceId(UUID.fromString(serviceId));
        entity.setName(request.name());
        entity.setScenarioIds(toJson(request.scenarioIds() == null ? List.of() : request.scenarioIds()));
        entity.setTcIds(toJson(request.tcIds() == null ? List.of() : request.tcIds()));
        groupRepository.save(entity);
        return toDomain(entity);
    }

    public ScenarioGroup update(String serviceId, String groupId, UpdateScenarioGroupRequest request) {
        ScenarioGroupEntity entity = requireEntity(serviceId, groupId);
        if (request.name() != null) {
            entity.setName(request.name());
        }
        groupRepository.save(entity);
        return toDomain(entity);
    }

    public void delete(String serviceId, String groupId) {
        ScenarioGroupEntity entity = requireEntity(serviceId, groupId);
        groupRepository.delete(entity);
    }

    public ScenarioGroup setSchedule(String serviceId, String groupId, CreateScheduleRequest request) {
        if (request.cron() == null || request.cron().isBlank()) {
            throw new QapilotException(ErrorCode.COMMON_001, "cron 필드가 필요합니다.");
        }
        ScenarioGroupEntity entity = requireEntity(serviceId, groupId);
        entity.setScheduleCron(request.cron());
        entity.setScheduleTimezone(request.timezone() == null ? "UTC" : request.timezone());
        entity.setScheduleEnabled(request.enabled() == null || request.enabled());
        entity.setScheduleCreatedAt(Instant.now());
        groupRepository.save(entity);
        return toDomain(entity);
    }

    private ScenarioGroupEntity requireEntity(String serviceId, String groupId) {
        try {
            Optional<ScenarioGroupEntity> opt = groupRepository.findById(UUID.fromString(groupId));
            if (opt.isEmpty() || !opt.get().getServiceId().equals(UUID.fromString(serviceId))) {
                throw new QapilotException(ErrorCode.SCENARIO_001);
            }
            return opt.get();
        } catch (IllegalArgumentException e) {
            throw new QapilotException(ErrorCode.SCENARIO_001);
        }
    }

    private ScenarioGroup toDomain(ScenarioGroupEntity e) {
        Schedule schedule = null;
        if (e.getScheduleCron() != null) {
            schedule = new Schedule(
                    e.getScheduleCron(),
                    e.getScheduleTimezone() == null ? "UTC" : e.getScheduleTimezone(),
                    e.isScheduleEnabled(),
                    e.getScheduleCreatedAt() == null ? null : e.getScheduleCreatedAt().toString()
            );
        }
        return new ScenarioGroup(
                e.getId().toString(),
                e.getName(),
                fromJson(e.getScenarioIds()),
                fromJson(e.getTcIds()),
                schedule,
                e.getCreatedAt() == null ? null : e.getCreatedAt().toString(),
                e.getUpdatedAt() == null ? null : e.getUpdatedAt().toString()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, STR_LIST);
        } catch (Exception ex) {
            return List.of();
        }
    }
}
