package com.qapilot.server.file.dto;

/**
 * 파일 메타 수정 요청.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record FileUpdateRequest(String name, Boolean reflected) {
}
