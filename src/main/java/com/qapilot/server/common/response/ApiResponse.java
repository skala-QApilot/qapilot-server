package com.qapilot.server.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * QApilot 공통 API 응답 포맷.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ApiError(code, message));
    }
}
