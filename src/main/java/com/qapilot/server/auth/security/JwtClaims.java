package com.qapilot.server.auth.security;

import java.time.Instant;
import java.util.Map;

/**
 * 검증된 JWT claim.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record JwtClaims(Map<String, Object> values) {

    public String subject() {
        return stringValue("sub");
    }

    public String email() {
        return stringValue("email");
    }

    public String role() {
        return stringValue("role");
    }

    public String type() {
        return stringValue("type");
    }

    public String jti() {
        return stringValue("jti");
    }

    public Instant expiresAt() {
        Object exp = values.get("exp");
        if (exp instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }
        return Instant.EPOCH;
    }

    private String stringValue(String key) {
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
