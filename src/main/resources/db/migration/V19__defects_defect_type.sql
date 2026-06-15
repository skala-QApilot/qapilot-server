-- ①장애유형(defect_type) 분리 저장 (metrics-measurement).
--
-- category 컬럼은 Layer3 ②결정분류(PRODUCT_DEFECT_CANDIDATE / TEST_DEFECT_* /
-- ENV_* 등)를 담도록 진화했다(V17/V18). 그 결과 product 결함의 ①장애유형
-- (UI/API/DATA/INFRA/DOMAIN)이 category 한 칸에 더 이상 담기지 못하고 소실된다.
-- 장애 분류 정확도 측정에는 ①장애유형과 ②결정분류가 모두 필요하므로 defect_type
-- 을 별도 컬럼으로 둔다.
--
-- nullable: 테스트/환경 분류(TEST_DEFECT_*, ENV_*) 행이나 단서가 없는 legacy 행은
-- ①장애유형이 존재하지 않는다.

ALTER TABLE defects ADD COLUMN defect_type VARCHAR(30)
    CHECK (defect_type IN ('UI_ERROR', 'API_ERROR', 'DATA_MISMATCH', 'INFRA', 'DOMAIN_RULE'));
