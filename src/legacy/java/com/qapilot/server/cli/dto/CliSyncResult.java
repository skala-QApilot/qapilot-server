package com.qapilot.server.cli.dto;

import java.util.List;

/**
 * CLI sync 결과 응답.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record CliSyncResult(int synced, int failed, List<String> paths, List<CliSyncItemError> errors) {
}
