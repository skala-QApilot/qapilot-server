package com.qapilot.server.auth.security;

import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import com.qapilot.server.common.files.QapilotPathResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * JWT secret을 환경변수 또는 .qapilot/auth/jwt-secret.key에서 공급한다.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class JwtSecretProvider {

    private static final String ENV_NAME = "QAPILOT_JWT_SECRET";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final QapilotPathResolver pathResolver;

    public JwtSecretProvider(QapilotPathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    public SecretKey secretKey() {
        String secret = secretValue();
        return new SecretKeySpec(secret.getBytes(), "HmacSHA256");
    }

    private String secretValue() {
        String envSecret = System.getenv(ENV_NAME);
        if (envSecret != null && !envSecret.isBlank()) {
            return envSecret;
        }
        return fileSecret();
    }

    private String fileSecret() {
        Path path = pathResolver.qapilotDir().resolve("auth").resolve("jwt-secret.key");
        try {
            if (Files.exists(path)) {
                return Files.readString(path).trim();
            }
            Files.createDirectories(path.getParent());
            String generated = generateSecret();
            Files.writeString(path, generated);
            return generated;
        } catch (IOException e) {
            throw new QapilotException(ErrorCode.SYSTEM_001, "JWT secret을 준비할 수 없습니다: " + path);
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
