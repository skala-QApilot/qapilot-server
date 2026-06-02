-- services / service_repos — 파일 기반 services.json 의 DB mirror.
--
-- 키 결정:
--   - (org_id, slug) unique — 같은 slug 가 다른 org 에 존재할 수 있다 (SaaS 전제).
--   - target_root / qapilot_dir 은 legacy 로컬 경로 — SaaS 컷오버 시 nullable 유지.
--   - service_repos 는 멀티레포 (현재 RepoConfig 리스트) 정규화.

CREATE TABLE services (
    id                  UUID PRIMARY KEY,
    org_id              UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    slug                VARCHAR(128) NOT NULL,
    display_name        VARCHAR(255) NOT NULL,
    description         TEXT,
    target_root         TEXT,                                     -- legacy 로컬 경로
    qapilot_dir         TEXT,                                     -- legacy 로컬 경로
    dashboard_url       TEXT,
    server_auth_token   VARCHAR(128) NOT NULL,
    token_issued_at     TIMESTAMPTZ NOT NULL,
    token_expires_at    TIMESTAMPTZ,
    staging_url         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (org_id, slug)
);

CREATE INDEX idx_services_org ON services(org_id);

CREATE TABLE service_repos (
    id              UUID PRIMARY KEY,
    service_id      UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    repo_url        TEXT NOT NULL,
    branch          VARCHAR(255),
    role            VARCHAR(64),
    token           TEXT,                                          -- TODO(phase-2): Secrets Manager 참조로 이동
    position        INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_service_repos_service ON service_repos(service_id);
