-- PR-15h — domain_documents 보강 (UI fileStore 매핑).

ALTER TABLE domain_documents ADD COLUMN file_id    UUID;
ALTER TABLE domain_documents ADD COLUMN reflected  BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE domain_documents ADD COLUMN mime_type  VARCHAR(128);

CREATE INDEX idx_domain_documents_file ON domain_documents(file_id);
