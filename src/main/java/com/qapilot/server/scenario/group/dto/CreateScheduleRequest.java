package com.qapilot.server.scenario.group.dto;

/**
 * 스케줄 생성 요청 DTO.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record CreateScheduleRequest(String cron, String timezone, Boolean enabled) {
}
