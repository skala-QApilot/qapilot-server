package com.qapilot.server.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 서비스 초기 설정 요청.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ServiceSetupRequest(String description, @JsonProperty("spec_files") List<String> specFiles) {
}
