package com.qapilot.server.cli.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * CLI sync source 메타데이터.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record CliSyncSource(
        @JsonProperty("target_root") String targetRoot,
        @JsonProperty("qapilot_dir") String qapilotDir
) {
}
