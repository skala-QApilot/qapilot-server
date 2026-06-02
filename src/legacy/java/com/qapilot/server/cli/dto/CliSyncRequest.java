package com.qapilot.server.cli.dto;

import java.util.List;

/**
 * CLI bulk sync 요청.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record CliSyncRequest<T>(CliSyncSource source, List<T> items) {
}
