-- rtm_versions / rtm_requirements / rtm_requirement_tc_links — 파일 기반 rtm-versions/*.json 의 DB mirror.
--
-- 키 결정:
--   - rtm_versions: 시나리오 생성 (또는 natural_lang 갱신) 마다 새 행 — 시나리오 버전과 1:1.
--   - rtm_requirements: 한 RTM 버전이 갖는 요구사항들. 다른 버전과는 독립 (history 는 version 단위).
--   - rtm_requirement_tc_links: TC 는 (ts_id, tc_id) 키. tc_results 와 JOIN 으로 status 계산.
--   - status / passCount / totalCount 는 저장 안 함 — Spring 이 응답 시점에 SQL 집계로 derive (phase D 종반).

CREATE TABLE rtm_versions (
    id          UUID PRIMARY KEY,
    service_id  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    label       VARCHAR(64) NOT NULL,
    trace_id    UUID,                                       -- 어느 시나리오 생성 trace 에서 만들어졌는지
    summary     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_rtm_versions_service_created ON rtm_versions(service_id, created_at DESC);

CREATE TABLE rtm_requirements (
    id              UUID PRIMARY KEY,
    rtm_version_id  UUID NOT NULL REFERENCES rtm_versions(id) ON DELETE CASCADE,
    req_id          VARCHAR(64) NOT NULL,
    content         TEXT,
    UNIQUE (rtm_version_id, req_id)
);

CREATE INDEX idx_rtm_requirements_version ON rtm_requirements(rtm_version_id);

CREATE TABLE rtm_requirement_tc_links (
    rtm_requirement_id  UUID NOT NULL REFERENCES rtm_requirements(id) ON DELETE CASCADE,
    ts_id               VARCHAR(64) NOT NULL,
    tc_id               VARCHAR(64) NOT NULL,
    PRIMARY KEY (rtm_requirement_id, ts_id, tc_id)
);

CREATE INDEX idx_rtm_links_tc ON rtm_requirement_tc_links(ts_id, tc_id);
