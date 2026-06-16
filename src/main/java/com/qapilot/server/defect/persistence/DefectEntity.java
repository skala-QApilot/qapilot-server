package com.qapilot.server.defect.persistence;

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

/**
 * defects 테이블. PR-19 (Phase 3) — DB + CRUD 만. Agent 자동 분류는 별도.
 *
 * <p>Author: C
 * <br>Created: 2026-06-04
 */
@Entity
@Table(name = "defects")
@Getter
@Setter
@NoArgsConstructor
public class DefectEntity {

    @Id
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "tc_result_id")
    private UUID tcResultId;

    @Column(name = "ts_id", nullable = false, length = 64)
    private String tsId;

    @Column(name = "tc_id", nullable = false, length = 64)
    private String tcId;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "root_cause_top1", columnDefinition = "text")
    private String rootCauseTop1;

    @Column(name = "root_cause_confidence", precision = 5, scale = 2)
    private BigDecimal rootCauseConfidence;

    @Column(name = "solution_guide", columnDefinition = "text")
    private String solutionGuide;

    @Column(length = 100)
    private String assignee;

    @Column(name = "file_location", columnDefinition = "text")
    private String fileLocation;

    @Column(name = "issue_url", columnDefinition = "text")
    private String issueUrl;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = "OPEN";
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
