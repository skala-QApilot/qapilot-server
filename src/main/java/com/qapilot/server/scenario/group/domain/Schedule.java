package com.qapilot.server.scenario.group.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 그룹 실행 스케줄.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Schedule(String cron, String timezone, boolean enabled, String createdAt) {
}
