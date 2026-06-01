-- 초기 스키마 — 멀티테넌트 SaaS 의 토대 (organizations / users / org_memberships / revoked_tokens).
--
-- 마이그레이션 정책:
--   - Flyway 가 스키마 단일 소유자. JPA ddl-auto=validate 로 매핑만 검증.
--   - 모든 PK 는 UUID (앱에서 생성, DB default 미사용 — 명시적이고 portable).
--   - 모든 timestamp 는 TIMESTAMPTZ. 앱은 Instant(UTC) 로 다룸.
--   - 이메일은 소문자로 저장하기로 약속 (앱에서 보장) → 단순 unique 인덱스.

-- ============================================================
-- organizations — SaaS 테넌트 단위. user 가 가입하면 자동 생성.
-- ============================================================
CREATE TABLE organizations (
    id          UUID PRIMARY KEY,
    slug        VARCHAR(64) NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    plan        VARCHAR(32) NOT NULL DEFAULT 'free',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- users — 글로벌 사용자. 한 사용자가 여러 org 에 속할 수 있음.
-- ============================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    hashed_password VARCHAR(255),                    -- nullable: OAuth-only 사용자 대비 (phase 2)
    name            VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at   TIMESTAMPTZ
);

-- ============================================================
-- org_memberships — user ↔ org N:M + 조직 내 역할.
-- ============================================================
CREATE TABLE org_memberships (
    id          UUID PRIMARY KEY,
    org_id      UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(32) NOT NULL,                -- owner | admin | member
    invited_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    joined_at   TIMESTAMPTZ,
    UNIQUE (org_id, user_id)
);

CREATE INDEX idx_org_memberships_user ON org_memberships (user_id);

-- ============================================================
-- revoked_tokens — JWT refresh token deny-list (기존 revoked-refresh-tokens.json 대체).
-- expires_at 지난 행은 정기 cleanup job 에서 삭제.
-- ============================================================
CREATE TABLE revoked_tokens (
    jti         VARCHAR(64) PRIMARY KEY,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_revoked_tokens_expires_at ON revoked_tokens (expires_at);
