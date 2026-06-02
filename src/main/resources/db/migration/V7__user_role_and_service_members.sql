-- PR-15h — Auth/Member 도메인 DB 전환 지원.
--
-- 1) users.role: 파일 기반 UserAccount.role ("admin" / "member") 를 DB 컬럼화.
--    기존 행은 default 'member' 로 채워지고, V8 backfill 도 없음 — 개별 사용자 admin 승급은 별도 운영.
--
-- 2) service_members: 한 서비스에 합류한 사용자 (file MemberFileStore 의 DB 대체).
--    org_memberships 와는 별개의 축 — 사용자가 본인 personal org 외에 다른 서비스에 합류한 경우.
--    role 은 owner/admin/member 정도. 현재는 모두 'member' 부여.

ALTER TABLE users ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT 'member';

CREATE TABLE service_members (
    service_id  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(32) NOT NULL DEFAULT 'member',
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (service_id, user_id)
);

CREATE INDEX idx_service_members_user ON service_members(user_id);
