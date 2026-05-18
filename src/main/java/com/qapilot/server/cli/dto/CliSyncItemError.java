package com.qapilot.server.cli.dto;

/**
 * CLI sync 항목별 오류.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record CliSyncItemError(int index, String message) {
}
