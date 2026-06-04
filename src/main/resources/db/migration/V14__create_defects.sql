-- Phase 3 — 결함 분석 / 해결 가이드 저장소.
--
-- TC FAIL 시 자동 분류 + 원인 Top1 + 해결 가이드 + 담당자 (Git blame) 를 저장한다.
-- 이번 마이그레이션은 DB + 기본 CRUD 만 도입. Agent 구현 (자동 분류) 은 별도 PR.

CREATE TABLE defects (
    id                    UUID PRIMARY KEY,
    service_id            UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    run_id                UUID NOT NULL REFERENCES runs(id) ON DELETE CASCADE,
    tc_result_id          UUID REFERENCES tc_results(id) ON DELETE SET NULL,
    ts_id                 VARCHAR(64) NOT NULL,
    tc_id                 VARCHAR(64) NOT NULL,
    category              VARCHAR(30) NOT NULL
                          CHECK (category IN ('UI_ERROR','API_ERROR','DATA_MISMATCH','INFRA','DOMAIN_RULE')),
    root_cause_top1       TEXT,
    root_cause_confidence NUMERIC(5,2),
    solution_guide        TEXT,
    assignee              VARCHAR(100),
    file_location         TEXT,
    status                VARCHAR(20) NOT NULL DEFAULT 'OPEN'
                          CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED')),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_defects_service_status ON defects(service_id, status);
CREATE INDEX idx_defects_run            ON defects(run_id);
