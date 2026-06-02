package com.qapilot.server.auth.store;

import com.qapilot.server.auth.persistence.RevokedTokenEntity;
import com.qapilot.server.auth.persistence.RevokedTokenRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * revoked_tokens 테이블 기반 refresh token 무효화 저장소. PR-15h — JPA 전환.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18, rewritten 2026-06-02
 */
@Component
public class RevokedRefreshTokenStore {

    private final RevokedTokenRepository revokedTokenRepository;

    public RevokedRefreshTokenStore(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public boolean isRevoked(String jti) {
        return revokedTokenRepository.existsById(jti);
    }

    public void revoke(String jti, Instant expiresAt) {
        // lazy cleanup — 만료된 deny-list 행 제거 (테이블 무한 성장 방지).
        revokedTokenRepository.deleteExpired(Instant.now());
        RevokedTokenEntity entity = new RevokedTokenEntity();
        entity.setJti(jti);
        entity.setExpiresAt(expiresAt);
        revokedTokenRepository.save(entity);
    }
}
