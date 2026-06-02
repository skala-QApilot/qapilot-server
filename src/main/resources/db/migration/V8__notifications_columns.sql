-- PR-15h — notifications 의 title/message/read_at 컬럼 추가.
-- 기존 content JSONB 는 추가 메타데이터 용도로 유지.

ALTER TABLE notifications ADD COLUMN title    VARCHAR(255);
ALTER TABLE notifications ADD COLUMN message  TEXT;
ALTER TABLE notifications ADD COLUMN read_at  TIMESTAMPTZ;
