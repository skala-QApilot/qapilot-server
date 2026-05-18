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
    FILE_001("FILE_001", "파일을 읽거나 쓸 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    AUTH_001("AUTH_001", "이메일 또는 비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    AUTH_002("AUTH_002", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    AUTH_003("AUTH_003", "토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    AUTH_004("AUTH_004", "권한이 부족합니다.", HttpStatus.FORBIDDEN),
    AUTH_005("AUTH_005", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    AUTH_006("AUTH_006", "이미 등록된 이메일입니다.", HttpStatus.CONFLICT),
    AUTH_007("AUTH_007", "초기 관리자 계정이 이미 생성되었습니다.", HttpStatus.FORBIDDEN),
    SERVICE_001("SERVICE_001", "서비스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SERVICE_002("SERVICE_002", "대상 경로를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST),
    SERVICE_003("SERVICE_003", "서비스 토큰이 유효하지 않습니다.", HttpStatus.UNAUTHORIZED),
    SCENARIO_001("SCENARIO_001", "시나리오를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SCENARIO_002("SCENARIO_002", "시나리오를 저장할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    RUN_001("RUN_001", "실행을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    AGENT_001("AGENT_001", "FastAPI Agent 호출에 실패했습니다.", HttpStatus.BAD_GATEWAY),
    AGENT_002("AGENT_002", "FastAPI 내부 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED),
    AGENT_003("AGENT_003", "FastAPI Agent 요청이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    RESULT_001("RESULT_001", "결과를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DASHBOARD_001("DASHBOARD_001", "대시보드 데이터를 집계할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

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
