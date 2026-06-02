package com.qapilot.server.service.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 서비스 멤버십 테이블. PR-15h — 기존 MemberFileStore(service/store) 의 DB 대체.
 *
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
@Entity
@Table(name = "service_members")
@IdClass(ServiceMemberEntity.PK.class)
@Getter
@Setter
@NoArgsConstructor
public class ServiceMemberEntity {

    @Id
    @Column(name = "service_id")
    private UUID serviceId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @PrePersist
    void onCreate() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }

    public static class PK implements Serializable {
        private UUID serviceId;
        private UUID userId;

        public PK() {}

        public PK(UUID serviceId, UUID userId) {
            this.serviceId = serviceId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK p)) return false;
            return Objects.equals(serviceId, p.serviceId) && Objects.equals(userId, p.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceId, userId);
        }
    }
}
