package com.qapilot.server.scenario.version.dto;

/**
 * 시나리오 버전 생성 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record CreateScenarioVersionRequest(String label, String description) {
}
