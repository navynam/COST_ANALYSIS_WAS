-- ============================================================
-- V3: 변경 요청, 파싱 노트 테이블 추가 + cost_formulas.departments 컬럼 추가
-- ============================================================

-- cost_formulas에 departments 컬럼 추가
ALTER TABLE cost_formulas ADD COLUMN IF NOT EXISTS departments JSONB DEFAULT '[]'::jsonb;

-- 변경 요청 (모델 워크플로우)
CREATE TABLE IF NOT EXISTS change_requests (
    id                    BIGSERIAL PRIMARY KEY,
    formula_id            BIGINT       NOT NULL REFERENCES cost_formulas(id) ON DELETE CASCADE,
    requester_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    requester_name        VARCHAR(50)  NOT NULL,
    department            VARCHAR(100),
    task_name             VARCHAR(200),
    original_formula      JSONB,
    modified_fields       JSONB,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    reason                TEXT,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reviewed_at           TIMESTAMPTZ,
    reviewer_comment      TEXT,
    approved_departments  JSONB
);

-- 파싱 노트
CREATE TABLE IF NOT EXISTS parsing_notes (
    id          BIGSERIAL PRIMARY KEY,
    file_id     BIGINT       NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(30)  NOT NULL DEFAULT 'parsing'
                CHECK (type IN ('parsing','upload','format','improvement')),
    content     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_change_req_formula   ON change_requests(formula_id);
CREATE INDEX IF NOT EXISTS idx_change_req_status    ON change_requests(status);
CREATE INDEX IF NOT EXISTS idx_change_req_requester ON change_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_parsing_notes_file   ON parsing_notes(file_id);
CREATE INDEX IF NOT EXISTS idx_parsing_notes_user   ON parsing_notes(user_id);
