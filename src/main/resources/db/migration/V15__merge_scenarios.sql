-- Phase 4 — 시나리오 도메인 통합.
--
-- AS-IS (3 테이블):
--   scenarios         (UUID id, ts_id, current_version_id → scenario_versions.id)
--   scenario_versions (UUID id, scenario_id → scenarios.id, version_number, payload) — 자동 history
--   scenario_snapshots (UUID id, label, scenarios_payload JSONB) — 사용자 마일스톤
--
-- TO-BE (2 테이블):
--   scenarios         (BIGSERIAL id, ts_id, version_number, payload, is_deleted) — 현재 작업 + 자동 이력 통합
--   scenario_versions (= 기존 scenario_snapshots rename) — 사용자 마일스톤 (의미 명확화)
--
-- latest 조회: WHERE service_id = ? AND is_deleted = false
--             SELECT DISTINCT ON (ts_id) ... ORDER BY ts_id, version_number DESC

-- (1) 새 scenarios 구조 (이름 충돌 회피 위해 임시 이름)
CREATE TABLE scenarios_unified (
    id             BIGSERIAL PRIMARY KEY,
    service_id     UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    ts_id          VARCHAR(64) NOT NULL,
    version_number INT NOT NULL,
    payload        JSONB NOT NULL,
    is_deleted     BOOLEAN NOT NULL DEFAULT false,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     UUID REFERENCES users(id),
    UNIQUE (service_id, ts_id, version_number)
);
CREATE INDEX idx_scenarios_unified_latest
    ON scenarios_unified(service_id, ts_id, version_number DESC);
CREATE INDEX idx_scenarios_unified_service ON scenarios_unified(service_id);

-- (2) 기존 데이터 마이그레이션 — scenarios + scenario_versions JOIN 으로 본문 확보
INSERT INTO scenarios_unified (service_id, ts_id, version_number, payload, created_at, created_by)
SELECT s.service_id, s.ts_id, sv.version_number, sv.payload, sv.created_at, sv.created_by
FROM scenarios s
JOIN scenario_versions sv ON sv.scenario_id = s.id
ORDER BY s.service_id, s.ts_id, sv.version_number;

-- (3) 기존 자동 history + HEAD 테이블 DROP.
--     scenarios.current_version_id ↔ scenario_versions.id 의 cycle FK 를 풀기 위해
--     scenario_versions 를 CASCADE 로 먼저 DROP (FK 자동 정리). 그 다음 scenarios.
DROP TABLE scenario_versions CASCADE;
DROP TABLE scenarios;

-- (4) 새 scenarios 로 rename + 인덱스 이름 정렬
ALTER TABLE scenarios_unified RENAME TO scenarios;
ALTER INDEX idx_scenarios_unified_latest  RENAME TO idx_scenarios_latest;
ALTER INDEX idx_scenarios_unified_service RENAME TO idx_scenarios_service;

-- (5) scenario_snapshots → scenario_versions (사용자 마일스톤의 의미 명확화)
ALTER TABLE scenario_snapshots RENAME TO scenario_versions;
ALTER INDEX idx_scenario_snapshots_service_created
    RENAME TO idx_scenario_versions_service_created;
