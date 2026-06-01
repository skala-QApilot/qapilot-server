package com.qapilot.server.run.persistence;

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
 * tc_results 테이블 — TC 별 실행 결과 (ui/api/db 별로 row).
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@Entity
@Table(name = "tc_results")
@Getter
@Setter
@NoArgsConstructor
public class TcResultEntity {

    @Id
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "ts_id", nullable = false)
    private String tsId;

    @Column(name = "tc_id", nullable = false)
    private String tcId;

    @Column(nullable = false)
    private String kind;                                  // ui / api / db

    private String status;                                // pass / fail / skip / null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    @Column(name = "artifact_count", nullable = false)
    private int artifactCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
