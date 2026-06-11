-- Layer 3 결정적 실패 분류 (qapilot agent) 도입에 따른 category 확장.
--
-- 기존 5종은 cross_check error_code 추론용. 신규 4종은 agent 의
-- _classify_failure 가 부여하는 1차 분류:
--   TEST_DEFECT_MAPPING      — 테스트 측 결함 (셀렉터/매핑) — SUT 결함 아님
--   ENV_TIMEOUT              — 사전조건/인프라로 인한 실행 타임아웃
--   ENV_UNVERIFIED           — API/DB 검증 미수행 (환경 설정)
--   PRODUCT_DEFECT_CANDIDATE — 상호작용 통과 후 assert 실패 (제품 결함 후보)
--
-- run 544ab04d: 신규 값이 본 CHECK 에 걸려 defects 0건 기록되던 격차 해소.

ALTER TABLE defects DROP CONSTRAINT defects_category_check;
ALTER TABLE defects ADD CONSTRAINT defects_category_check
    CHECK (category IN (
        'UI_ERROR', 'API_ERROR', 'DATA_MISMATCH', 'INFRA', 'DOMAIN_RULE',
        'TEST_DEFECT_MAPPING', 'TEST_DEFECT_UNVERIFIABLE', 'ENV_TIMEOUT', 'ENV_UNVERIFIED', 'PRODUCT_DEFECT_CANDIDATE'
    ));
