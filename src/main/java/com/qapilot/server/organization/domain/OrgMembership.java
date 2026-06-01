package com.qapilot.server.organization.domain;

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
 * user ↔ organization N:M 매핑 + 조직 내 역할.
 *
 * <p>Author: C
 * <br>Created: 2026-06-01
 */
@Entity
@Table(name = "org_memberships")
@Getter
@Setter
@NoArgsConstructor
public class OrgMembership {

    @Id
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String role;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @PrePersist
    void onCreate() {
        if (invitedAt == null) {
            invitedAt = Instant.now();
        }
    }
}
