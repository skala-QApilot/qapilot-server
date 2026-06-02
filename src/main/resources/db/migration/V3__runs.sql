-- runs / tc_results / tc_artifacts — traces/*.json + results/<trace>/.../result.json + screenshots 의 DB mirror.
--
-- 정책:
--   - runs.id == trace_id (UUID 그대로 재사용 — resume 시 동일 trace_id 유지)
--   - JSONB 4종: options(실행 옵션), summary(P/F/N 집계), agent_logs(이벤트 로그), payload(개별 TC 결과)
--   - 스크린샷/HTML 등 binary 는 tc_artifacts 에서 s3_key 만 보관 (S3 가 실제 바이트)
--   - tc_results UNIQUE(run_id, ts_id, tc_id, kind) — 같은 TC 의 ui/api/db 결과는 별 행
--   - agent_logs 가 커지면 phase 2 에서 run_events 별도 테이블로 분리

CREATE TABLE runs (
    id                          UUID PRIMARY KEY,
    service_id                  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    triggered_by                UUID REFERENCES users(id),
    command                     VARCHAR(64) NOT NULL,
    trigger                     VARCHAR(64) NOT NULL,
    status                      VARCHAR(32) NOT NULL,           -- queued / running / completed / aborted / failed
    started_at                  TIMESTAMPTZ NOT NULL,
    completed_at                TIMESTAMPTZ,
    error                       TEXT,
    confidence                  NUMERIC(5,4),
    total_cost                  NUMERIC(12,6),
    selected_total_tc_count     INT,
    options                     JSONB,
    summary                     JSONB,
    agent_logs                  JSONB,
    task_id                     VARCHAR(128),                   -- phase 2 Celery task id 자리
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_runs_service_started ON runs (service_id, started_at DESC);
CREATE INDEX idx_runs_status ON runs (status);

CREATE TABLE tc_results (
    id              UUID PRIMARY KEY,
    run_id          UUID NOT NULL REFERENCES runs(id) ON DELETE CASCADE,
    ts_id           VARCHAR(64) NOT NULL,
    tc_id           VARCHAR(64) NOT NULL,
    kind            VARCHAR(16) NOT NULL,                       -- ui / api / db
    status          VARCHAR(16),                                -- pass / fail / skip
    payload         JSONB,
    artifact_count  INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (run_id, ts_id, tc_id, kind)
);

CREATE INDEX idx_tc_results_run_kind ON tc_results (run_id, kind);
CREATE INDEX idx_tc_results_run_status ON tc_results (run_id, status);

CREATE TABLE tc_artifacts (
    id              UUID PRIMARY KEY,
    tc_result_id    UUID NOT NULL REFERENCES tc_results(id) ON DELETE CASCADE,
    step_index      INT NOT NULL,
    kind            VARCHAR(16) NOT NULL,                       -- png / html
    s3_key          TEXT NOT NULL,
    bytes           BIGINT,
    sha256          VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tc_artifacts_result ON tc_artifacts (tc_result_id, step_index);
