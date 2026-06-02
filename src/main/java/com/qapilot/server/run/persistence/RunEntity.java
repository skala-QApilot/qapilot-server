package com.qapilot.server.run.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * runs 테이블 JPA 엔티티 — traces/*.json 의 DB mirror.
 *
 * <p>id == trace_id (FastAPI 가 발급한 UUID 그대로 재사용 — resume 동일 trace_id 유지).
 *
 * <p>JSONB 컬럼은 raw JSON 문자열로 매핑. 구조는 컨슈머가 Jackson 으로 직접 파싱.
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@Entity
@Table(name = "runs")
@Getter
@Setter
@NoArgsConstructor
public class RunEntity {

    @Id
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(nullable = false)
    private String command;

    @Column(name = "trigger", nullable = false)
    private String trigger;

    @Column(nullable = false)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(columnDefinition = "text")
    private String error;

    private BigDecimal confidence;

    @Column(name = "total_cost")
    private BigDecimal totalCost;

    @Column(name = "selected_total_tc_count")
    private Integer selectedTotalTcCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String options;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "agent_logs", columnDefinition = "jsonb")
    private String agentLogs;

    @Column(name = "task_id")
    private String taskId;

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
