package com.qapilot.server.auth.security;

import java.security.MessageDigest;

/**
 * 보안 비교 헬퍼.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
final class MessageDigestHelper {

    private MessageDigestHelper() {
    }

    static boolean equals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }
}
