-- qapilot_dir 은 target_root + '/.qapilot/' + slug 으로 항상 계산 가능하므로 컬럼 제거.
ALTER TABLE services DROP COLUMN IF EXISTS qapilot_dir;
