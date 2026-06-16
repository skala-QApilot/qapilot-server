-- defect 별 생성된 GitHub issue URL 저장 컬럼.

ALTER TABLE defects ADD COLUMN issue_url TEXT;
