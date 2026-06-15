package com.qapilot.server.slack.dto;

/**
 * Slack DM 파일 전송 결과 — 수신자별 상태.
 *
 * <p>status: sent / not_found / failed
 *
 * <p>Author: C
 * <br>Created: 2026-06-15
 */
public record SlackNotifyResult(String email, String status) {
}
