package com.qapilot.server.cli.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 생성 코드 sync 항목.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record GeneratedCodeSyncItem(@JsonProperty("tc_id") String tcId, String path, String code) {
}
