-- Phase 1 정리 — dead schema + 사용자가 폐기 결정한 그룹/재테스트 테이블 제거.
--
-- (1) domain_glossary / domain_rules
--     실제 도메인 지식은 Qdrant 가 담당. 이 두 테이블은 0 row + 채우는 코드 0건.
-- (2) scenario_group_members
--     scenario_groups.scenario_ids (JSONB) 와 완전 중복. Spring 은 이미 JSONB 만 사용.
-- (3) retest_group_scenarios / retest_groups
--     재테스트 기능은 향후 도메인 맞춰 재설계 예정. 현재 0 row + 채우는 코드 0건.

DROP TABLE IF EXISTS domain_glossary;
DROP TABLE IF EXISTS domain_rules;

DROP TABLE IF EXISTS scenario_group_members;

DROP TABLE IF EXISTS retest_group_scenarios;
DROP TABLE IF EXISTS retest_groups;
