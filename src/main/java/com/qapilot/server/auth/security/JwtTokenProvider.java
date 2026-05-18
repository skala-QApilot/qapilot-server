package com.qapilot.server.auth.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qapilot.server.auth.domain.UserAccount;
import com.qapilot.server.common.error.ErrorCode;
import com.qapilot.server.common.error.QapilotException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import org.springframework.stereotype.Component;

/**
 * HS256 JWT 발급/검증 유틸.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@Component
public class JwtTokenProvider {

    private static final Duration ACCESS_TTL = Duration.ofMinutes(60);
    private static final Duration REFRESH_TTL = Duration.ofDays(7);
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final JwtSecretProvider secretProvider;
    private final ObjectMapper objectMapper;

    public JwtTokenProvider(JwtSecretProvider secretProvider, ObjectMapper objectMapper) {
        this.secretProvider = secretProvider;
        this.objectMapper = objectMapper;
    }

    public String createAccessToken(UserAccount user) {
        Map<String, Object> payload = basePayload(user.userId(), "access", ACCESS_TTL);
        payload.put("email", user.email());
        payload.put("role", user.role());
        return createToken(payload);
    }

    public String createRefreshToken(UserAccount user) {
        Map<String, Object> payload = basePayload(user.userId(), "refresh", REFRESH_TTL);
        payload.put("jti", UUID.randomUUID().toString());
        return createToken(payload);
    }

    public JwtClaims verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3 || !constantTimeEquals(signature(parts[0] + "." + parts[1]), parts[2])) {
            throw new QapilotException(ErrorCode.AUTH_003);
        }
        JwtClaims claims = new JwtClaims(readPayload(parts[1]));
        if (claims.expiresAt().isBefore(Instant.now())) {
            throw new QapilotException(ErrorCode.AUTH_002);
        }
        return claims;
    }

    private Map<String, Object> basePayload(String subject, String type, Duration ttl) {
        Instant now = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", subject);
        payload.put("type", type);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plus(ttl).getEpochSecond());
        return payload;
    }

    private String createToken(Map<String, Object> payload) {
        String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
        String body = encodeJson(payload);
        return header + "." + body + "." + signature(header + "." + body);
    }

    private String encodeJson(Object value) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.AUTH_003);
        }
    }

    private Map<String, Object> readPayload(String payload) {
        try {
            byte[] bytes = URL_DECODER.decode(payload);
            return objectMapper.readValue(bytes, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.AUTH_003);
        }
    }

    private String signature(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretProvider.secretKey());
            return URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new QapilotException(ErrorCode.AUTH_003);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigestHelper.equals(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
