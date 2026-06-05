package com.qapilot.server.service.persistence;

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

/**
 * services 테이블 JPA 엔티티 — services.json 과 dual-write.
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@Entity
@Table(name = "services")
@Getter
@Setter
@NoArgsConstructor
public class ServiceEntity {

    @Id
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false)
    private String slug;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    private String description;

    @Column(name = "target_root")
    private String targetRoot;

    @Column(name = "dashboard_url")
    private String dashboardUrl;

    @Column(name = "server_auth_token", nullable = false)
    private String serverAuthToken;

    @Column(name = "token_issued_at", nullable = false)
    private Instant tokenIssuedAt;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "staging_url")
    private String stagingUrl;

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
