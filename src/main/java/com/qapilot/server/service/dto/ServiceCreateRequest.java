package com.qapilot.server.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qapilot.server.service.domain.RepoConfig;
import java.util.List;

/**
 * 서비스 생성 요청.
 *
 * <p>repos / stagingUrl 은 GitHub/GitLab 통합용 (선택). 입력 시 서비스 메타에 저장되어
 * 후속 Agent 실행에서 자동 활용된다. repos 는 멀티 레포 지원.
 *
 * <p>Author: C
 * <br>Created: 2026-05-18
 */
public record ServiceCreateRequest(
        String name,
        String description,
        @JsonProperty("target_root") String targetRoot,
        @JsonProperty("repos") List<RepoConfig> repos,
        @JsonProperty("staging_url") String stagingUrl
) {
}
