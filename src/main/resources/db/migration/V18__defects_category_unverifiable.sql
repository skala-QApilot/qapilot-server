-- TEST_DEFECT_UNVERIFIABLE 분류 추가 (run feb0dc5e 축 ③).
--
-- 서술형 then 텍스트 assert 실패 = 검증 표현력 한계 — 제품 결함이 아니라
-- TC then 절을 관찰 가능한 결과로 구체화해야 하는 테스트 자산 이슈.
--
-- (별도 V18 인 이유: V17 은 이미 적용됨 — 적용된 마이그레이션 파일 수정은
-- Flyway checksum mismatch 로 기동 실패를 유발한다.)

ALTER TABLE defects DROP CONSTRAINT defects_category_check;
ALTER TABLE defects ADD CONSTRAINT defects_category_check
    CHECK (category IN (
        'UI_ERROR', 'API_ERROR', 'DATA_MISMATCH', 'INFRA', 'DOMAIN_RULE',
        'TEST_DEFECT_MAPPING', 'TEST_DEFECT_UNVERIFIABLE',
        'ENV_TIMEOUT', 'ENV_UNVERIFIED', 'PRODUCT_DEFECT_CANDIDATE'
    ));
