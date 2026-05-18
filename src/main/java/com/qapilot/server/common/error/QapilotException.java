package com.qapilot.server.common.error;

/**
 * QApilot 서버 공통 런타임 예외.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public class QapilotException extends RuntimeException {

    private final ErrorCode errorCode;

    public QapilotException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public QapilotException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
