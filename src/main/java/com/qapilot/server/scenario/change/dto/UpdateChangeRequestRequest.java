package com.qapilot.server.scenario.change.dto;

/**
 * 변경 요청 수정 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record UpdateChangeRequestRequest(String status, String reviewer) {
}
