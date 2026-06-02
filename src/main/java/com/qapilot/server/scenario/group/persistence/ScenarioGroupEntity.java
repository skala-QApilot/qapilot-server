package com.qapilot.server.scenario.group.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * scenario_groups 테이블. PR-16 — V11 으로 스케줄/tc_ids 컬럼 확장.
 *
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
@Entity
@Table(name = "scenario_groups")
@Getter
@Setter
@NoArgsConstructor
public class ScenarioGroupEntity {

    @Id
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scenario_ids", columnDefinition = "jsonb")
    private String scenarioIds;      // JSON 배열 문자열

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tc_ids", columnDefinition = "jsonb")
    private String tcIds;            // JSON 배열 문자열

    @Column(name = "schedule_cron")
    private String scheduleCron;

    @Column(name = "schedule_timezone")
    private String scheduleTimezone;

    @Column(name = "schedule_enabled", nullable = false)
    private boolean scheduleEnabled;

    @Column(name = "schedule_created_at")
    private Instant scheduleCreatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
