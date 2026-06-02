-- PR-15h — change_requests 를 scenario 변경 요청 모델에 맞게 컬럼 보강.
-- 기존 type/target_id/content 는 nullable 로 완화 (보존만, 신규 행은 안 채움).

ALTER TABLE change_requests ALTER COLUMN type DROP NOT NULL;
ALTER TABLE change_requests ALTER COLUMN target_id DROP NOT NULL;

ALTER TABLE change_requests ADD COLUMN scenario_id  VARCHAR(64);
ALTER TABLE change_requests ADD COLUMN reason       TEXT;
ALTER TABLE change_requests ADD COLUMN trigger      VARCHAR(64);
ALTER TABLE change_requests ADD COLUMN reviewer     VARCHAR(255);
ALTER TABLE change_requests ADD COLUMN reviewed_at  TIMESTAMPTZ;

CREATE INDEX idx_change_requests_service_scenario ON change_requests(service_id, scenario_id);
