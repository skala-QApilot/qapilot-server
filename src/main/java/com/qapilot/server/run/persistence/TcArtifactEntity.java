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

/**
 * tc_artifacts 테이블 — 스크린샷·HTML·기타 binary 의 메타데이터.
 *
 * <p>실제 바이트는 S3({@code s3_key}) 에 있고 row 는 가벼움. sha256 dedup 가능.
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@Entity
@Table(name = "tc_artifacts")
@Getter
@Setter
@NoArgsConstructor
public class TcArtifactEntity {

    @Id
    private UUID id;

    @Column(name = "tc_result_id", nullable = false)
    private UUID tcResultId;

    @Column(name = "step_index", nullable = false)
    private int stepIndex;

    @Column(nullable = false)
    private String kind;                              // png / html

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    private Long bytes;

    private String sha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
