package com.qapilot.server.auth.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qapilot.server.auth.domain.RevokedRefreshToken;
import com.qapilot.server.common.files.JsonFileStore;
import com.qapilot.server.common.files.QapilotPathResolver;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * revoked-refresh-tokens.json 파일 기반 refresh token 무효화 저장소.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class RevokedRefreshTokenStore {

    private final JsonFileStore jsonFileStore;
    private final QapilotPathResolver pathResolver;

    public RevokedRefreshTokenStore(JsonFileStore jsonFileStore, QapilotPathResolver pathResolver) {
        this.jsonFileStore = jsonFileStore;
        this.pathResolver = pathResolver;
    }

    public boolean isRevoked(String jti) {
        return loadActiveTokens().stream().anyMatch(token -> token.jti().equals(jti));
    }

    public void revoke(String jti, Instant expiresAt) {
        List<RevokedRefreshToken> tokens = loadActiveTokens();
        tokens.add(new RevokedRefreshToken(jti, expiresAt.toString()));
        jsonFileStore.write(tokensPath(), tokens);
    }

    private List<RevokedRefreshToken> loadActiveTokens() {
        Instant now = Instant.now();
        List<RevokedRefreshToken> tokens = new ArrayList<>(jsonFileStore.readOrDefault(tokensPath(), new TypeReference<>() {
        }, List.of()));
        List<RevokedRefreshToken> active = tokens.stream()
                .filter(token -> Instant.parse(token.expiresAt()).isAfter(now))
                .toList();
        if (active.size() != tokens.size()) {
            jsonFileStore.write(tokensPath(), active);
        }
        return new ArrayList<>(active);
    }

    private Path tokensPath() {
        return pathResolver.qapilotDir().resolve("auth").resolve("revoked-refresh-tokens.json");
    }
}
