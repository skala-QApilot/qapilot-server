package com.qapilot.server.service.persistence;

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
 * service_repos 테이블 — 한 서비스가 갖는 다중 Git 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@Entity
@Table(name = "service_repos")
@Getter
@Setter
@NoArgsConstructor
public class ServiceRepoEntity {

    @Id
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "repo_url", nullable = false)
    private String repoUrl;

    private String branch;

    private String role;

    private String token;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
