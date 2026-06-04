-- Phase 2 — DB session timezone Asia/Seoul.
--
-- 한국 사용자 only 정책. TIMESTAMPTZ 컬럼은 유지 (정보 보존) — session timezone 만 변경.
-- 새 connection 부터 적용. 새 row 의 DEFAULT now() 는 KST 시각, 표시도 KST.
-- 기존 row 는 저장된 시각 그대로 + KST 로 해석.
--
-- Spring/FastAPI 측에서도 jackson.time-zone, hibernate.jdbc.time_zone, FastAPI datetime helper 가
-- KST 로 정렬되도록 별도 설정 (application.yml + shared/*).

ALTER DATABASE qapilot SET timezone = 'Asia/Seoul';
