package com.qapilot.server.common.error;

import org.springframework.http.HttpStatus;

/**
 * Spring Boot 중앙 API 서버 공통 에러 코드.
 *
 * <p>Prefix policy:
 * COMMON_XXX: 공통 요청/서버 오류
 * SYSTEM_XXX: 설정/환경 오류
 * FILE_XXX: 파일 저장소 오류
 * AUTH_XXX, SERVICE_XXX, SCENARIO_XXX, AGENT_XXX: 후속 도메인 API에서 확장
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public enum ErrorCode {
    COMMON_001("COMMON_001", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    COMMON_002("COMMON_002", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COMMON_003("COMMON_003", "서버 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    SYSTEM_001("SYSTEM_001", ".qapilot 경로를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    FILE_001("FILE_001", "파일을 읽거나 쓸 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;

    ErrorCode(String code, String defaultMessage, HttpStatus status) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.status = status;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }
}
