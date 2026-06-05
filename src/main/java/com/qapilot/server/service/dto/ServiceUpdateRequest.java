package com.qapilot.server.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qapilot.server.service.domain.RepoConfig;
import java.util.List;

/**
 * 서비스 수정 요청.
 *
 * <p>repos/stagingUrl 은 설정 화면 수정용 — null 이면 해당 항목 미변경.
 * repos 의 token 이 blank 면 기존 PAT 를 보존한다(UI 는 PAT 를 재전송하지 않음).
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ServiceUpdateRequest(
        String name,
        String description,
        List<RepoConfig> repos,
        @JsonProperty("staging_url") String stagingUrl
) {
}
