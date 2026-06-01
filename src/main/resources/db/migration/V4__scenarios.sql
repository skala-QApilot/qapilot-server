-- scenarios / scenario_versions / scenario_groups — 파일 기반 .qapilot/<slug>/scenarios/*.json 의 DB mirror.
--
-- 버전 정책:
--   - scenarios 한 행은 (service_id, ts_id) 의 "현재 상태" 가리킨다 (current_version_id 포인터).
--   - scenario_versions 는 immutable history — _save_scenarios 호출마다 새 version_number 부여.
--   - current_version_id 는 nullable — 첫 INSERT 시점에 아직 version 행이 없어 NULL 허용 (이후 UPDATE).
--   - RTM 버전도 시나리오 버전과 1:1 (요구사항 매핑이 시나리오 변경마다 바뀔 수 있음, phase D PR-12).

CREATE TABLE scenarios (
    id                  UUID PRIMARY KEY,
    service_id          UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    ts_id               VARCHAR(64) NOT NULL,
    current_version_id  UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (service_id, ts_id)
);

CREATE INDEX idx_scenarios_service ON scenarios(service_id);

CREATE TABLE scenario_versions (
    id              UUID PRIMARY KEY,
    scenario_id     UUID NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    version_number  INT NOT NULL,
    payload         JSONB NOT NULL,                       -- {description, test_cases:[], depends_on, ...}
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      UUID REFERENCES users(id),
    UNIQUE (scenario_id, version_number)
);

-- 순환 FK 피하기: scenarios.current_version_id 는 scenario_versions.id 를 참조하지만,
-- 첫 시나리오 INSERT 시 version 행이 아직 없으므로 nullable. 이후 ALTER 로 FK 걸어도 됨.
ALTER TABLE scenarios
    ADD CONSTRAINT fk_scenarios_current_version
    FOREIGN KEY (current_version_id) REFERENCES scenario_versions(id) ON DELETE SET NULL;

CREATE INDEX idx_scenario_versions_scenario_version ON scenario_versions(scenario_id, version_number DESC);

CREATE TABLE scenario_groups (
    id          UUID PRIMARY KEY,
    service_id  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_scenario_groups_service ON scenario_groups(service_id);

CREATE TABLE scenario_group_members (
    group_id    UUID NOT NULL REFERENCES scenario_groups(id) ON DELETE CASCADE,
    scenario_id UUID NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    position    INT NOT NULL DEFAULT 0,
    PRIMARY KEY (group_id, scenario_id)
);
