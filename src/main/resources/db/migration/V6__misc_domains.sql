-- 나머지 도메인 — 파일 기반 .qapilot/<slug>/{change-requests,retest-groups,notifications,
-- generated-code,action-mappings,reports,codebase-index,domain}/ 의 DB+S3 mirror.
--
-- 정책:
--   - 작은 JSON 메타는 JSONB 컬럼, 큰 텍스트/binary 는 S3 (s3_key 만 보관).
--   - dual-write hook 은 PR-13 범위에선 schema 만. 도메인별 writer 는 후속 PR.
--   - 모든 테이블 service_id FK → org_id 격리는 services.org_id JOIN 으로 자연 보장.

-- ============================================================
-- 변경 요청 (시나리오 수정 / 코드 변경 등 워크플로 트리거)
-- ============================================================
CREATE TABLE change_requests (
    id          UUID PRIMARY KEY,
    service_id  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    type        VARCHAR(64) NOT NULL,                       -- scenario_edit / code_change / retest / ...
    target_id   VARCHAR(128) NOT NULL,
    content     JSONB,
    status      VARCHAR(32) NOT NULL DEFAULT 'pending',     -- pending / accepted / rejected / done
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_change_requests_service_status ON change_requests(service_id, status);

-- ============================================================
-- 재테스트 그룹 + 멤버 시나리오
-- ============================================================
CREATE TABLE retest_groups (
    id          UUID PRIMARY KEY,
    service_id  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    status      VARCHAR(32) NOT NULL DEFAULT 'pending',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_retest_groups_service ON retest_groups(service_id, created_at DESC);

CREATE TABLE retest_group_scenarios (
    group_id    UUID NOT NULL REFERENCES retest_groups(id) ON DELETE CASCADE,
    scenario_id UUID NOT NULL REFERENCES scenarios(id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, scenario_id)
);

-- ============================================================
-- 알림
-- ============================================================
CREATE TABLE notifications (
    id          UUID PRIMARY KEY,
    service_id  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    user_id     UUID REFERENCES users(id),                  -- nullable: 서비스 전체 broadcast
    type        VARCHAR(64) NOT NULL,
    content     JSONB,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_service_unread ON notifications(service_id, is_read, created_at DESC);

-- ============================================================
-- 생성 코드 (.js) — S3
-- ============================================================
CREATE TABLE generated_code (
    id          UUID PRIMARY KEY,
    service_id  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    tc_id       VARCHAR(64) NOT NULL,
    version     INT NOT NULL,
    s3_key      TEXT NOT NULL,
    bytes       BIGINT,
    sha256      VARCHAR(64),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (service_id, tc_id, version)
);

CREATE INDEX idx_generated_code_service_tc ON generated_code(service_id, tc_id, version DESC);

-- ============================================================
-- 액션 매핑 (작은 JSON) — JSONB 인라인
-- ============================================================
CREATE TABLE action_mappings (
    id          UUID PRIMARY KEY,
    service_id  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    tc_id       VARCHAR(64) NOT NULL,
    version     INT NOT NULL,
    payload     JSONB NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (service_id, tc_id, version)
);

CREATE INDEX idx_action_mappings_service_tc ON action_mappings(service_id, tc_id, version DESC);

-- ============================================================
-- 리포트 (markdown) — S3
-- ============================================================
CREATE TABLE reports (
    id           UUID PRIMARY KEY,
    run_id       UUID NOT NULL UNIQUE REFERENCES runs(id) ON DELETE CASCADE,
    s3_key       TEXT NOT NULL,
    bytes        BIGINT,
    sha256       VARCHAR(64),
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- 코드베이스 인덱스 — endpoints / models / functions / callgraph / manifest / frontend
-- ============================================================
CREATE TABLE codebase_indices (
    id           UUID PRIMARY KEY,
    service_id   UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    commit_hash  VARCHAR(64),
    kind         VARCHAR(32) NOT NULL,                      -- endpoints / models / functions / callgraph / manifest / frontend
    s3_key       TEXT NOT NULL,
    bytes        BIGINT,
    sha256       VARCHAR(64),
    file_count   INT,
    scanned_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_codebase_indices_service_commit ON codebase_indices(service_id, commit_hash, kind);

-- ============================================================
-- 도메인 문서 (원본 PDF/DOC 등) — S3
-- ============================================================
CREATE TABLE domain_documents (
    id           UUID PRIMARY KEY,
    service_id   UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    filename     VARCHAR(255) NOT NULL,
    version      INT NOT NULL DEFAULT 1,
    s3_key       TEXT NOT NULL,
    bytes        BIGINT,
    sha256       VARCHAR(64),
    uploaded_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_domain_documents_service ON domain_documents(service_id, uploaded_at DESC);

-- ============================================================
-- 도메인 용어집 (code_name → business_name)
-- ============================================================
CREATE TABLE domain_glossary (
    id            UUID PRIMARY KEY,
    service_id    UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    code_name     VARCHAR(255) NOT NULL,
    business_name VARCHAR(255) NOT NULL,
    UNIQUE (service_id, code_name)
);

-- ============================================================
-- 도메인 규칙 (LLM 문맥 주입용)
-- ============================================================
CREATE TABLE domain_rules (
    id          UUID PRIMARY KEY,
    service_id  UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    req_id      VARCHAR(64) NOT NULL,
    content     TEXT NOT NULL,
    area        VARCHAR(128),
    priority    VARCHAR(32),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_domain_rules_service_req ON domain_rules(service_id, req_id);
