package com.qapilot.server.common.response;

/**
 * 공통 실패 응답 error 객체.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ApiError(String code, String message) {
}
