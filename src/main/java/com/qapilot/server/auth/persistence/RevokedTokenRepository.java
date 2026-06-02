package com.qapilot.server.auth.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Author: C
 * <br>Created: 2026-06-02
 */
public interface RevokedTokenRepository extends JpaRepository<RevokedTokenEntity, String> {

    /** 만료된 deny-list 정리. revoke 호출 시 lazy cleanup 용. */
    @Modifying
    @Transactional
    @Query("DELETE FROM RevokedTokenEntity r WHERE r.expiresAt < :now")
    int deleteExpired(Instant now);
}
