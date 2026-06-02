-- PR-16 — file-based 잔존 도메인 DB 전환.
--
-- (1) scenario_snapshots — 사용자가 명시 저장하는 "전체 시나리오 집합의 한 시점 통째 스냅샷".
--     DB 의 scenario_versions (시나리오 한 개의 자동 변경 이력) 과는 이름은 비슷하지만 다른 개념.
--     UI 의 "버전 저장" / 즐겨찾기 토글 기능 백엔드.
--
-- (2) scenario_groups 에 스케줄 컬럼 + tc_ids JSONB 추가.
--     ScenarioGroup.schedule (cron / timezone / enabled / createdAt) 를 풀어서 인라인.

CREATE TABLE scenario_snapshots (
    id                 UUID PRIMARY KEY,
    service_id         UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    label              VARCHAR(255) NOT NULL,
    description        TEXT,
    scenarios_payload  JSONB NOT NULL,                       -- 그 시점 시나리오들 통째
    is_favorite        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_scenario_snapshots_service_created ON scenario_snapshots(service_id, created_at DESC);

ALTER TABLE scenario_groups ADD COLUMN scenario_ids        JSONB;
ALTER TABLE scenario_groups ADD COLUMN tc_ids              JSONB;
ALTER TABLE scenario_groups ADD COLUMN schedule_cron       VARCHAR(64);
ALTER TABLE scenario_groups ADD COLUMN schedule_timezone   VARCHAR(64);
ALTER TABLE scenario_groups ADD COLUMN schedule_enabled    BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE scenario_groups ADD COLUMN schedule_created_at TIMESTAMPTZ;
ALTER TABLE scenario_groups ADD COLUMN updated_at          TIMESTAMPTZ NOT NULL DEFAULT now();
