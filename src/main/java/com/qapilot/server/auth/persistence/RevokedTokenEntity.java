package com.qapilot.server.auth.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * revoked_tokens 테이블 — JWT refresh token deny-list. PR-15h.
 *
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
@Entity
@Table(name = "revoked_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RevokedTokenEntity {

    @Id
    private String jti;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    @PrePersist
    void onCreate() {
        if (revokedAt == null) {
            revokedAt = Instant.now();
        }
    }
}
