package com.qapilot.server.scenario.version.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * scenario_versions 테이블 — 사용자 명시 마일스톤.
 *
 * <p>"service 의 시나리오 N개를 통째로 박제한 한 시점" 을 저장한다. label/description/즐겨찾기 메타.
 * 자동 작업 이력 (per-scenario revision) 은 scenarios 테이블의 version_number 컬럼이 담당한다.
 *
 * <p>Author: C
 * <br>Created: 2026-06-02, renamed 2026-06-04 (V15)
 */
@Entity
@Table(name = "scenario_versions")
@Getter
@Setter
@NoArgsConstructor
public class ScenarioVersionEntity {

    @Id
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(nullable = false)
    private String label;

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scenarios_payload", nullable = false, columnDefinition = "jsonb")
    private String scenariosPayload;

    @Column(name = "is_favorite", nullable = false)
    private boolean isFavorite;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
