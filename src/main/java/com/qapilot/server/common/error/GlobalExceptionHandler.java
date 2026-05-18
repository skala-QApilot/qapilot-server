package com.qapilot.server.common.error;

import com.qapilot.server.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외를 공통 응답 포맷으로 변환한다.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(QapilotException.class)
    public ResponseEntity<ApiResponse<Void>> handleQapilotException(QapilotException exception) {
        ErrorCode code = exception.errorCode();
        return ResponseEntity
                .status(code.status())
                .body(ApiResponse.fail(code.code(), exception.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.COMMON_001.code(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.COMMON_003.code(), ErrorCode.COMMON_003.defaultMessage()));
    }
}
