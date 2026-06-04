-- scenario_versions (사용자 마일스톤) 의 (service_id, label) UNIQUE.
--
-- onReviewConfirm 이 v(N+1) 자동 증가 라벨로 박제하지만, 동시 호출 / 자동 계산 미스로
-- 같은 라벨 두 번 박힐 위험을 DB 단에서 차단. 사용자가 의도적으로 같은 이름 재사용하려면
-- 기존 row 를 먼저 삭제해야 함 (의도된 마찰).

ALTER TABLE scenario_versions
    ADD CONSTRAINT scenario_versions_service_label_key UNIQUE (service_id, label);
