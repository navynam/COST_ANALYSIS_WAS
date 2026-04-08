-- ============================================================
-- cost-analysis-backend  초기 스키마
-- ============================================================

-- 사용자
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     VARCHAR(20)  UNIQUE NOT NULL,
    password        VARCHAR(255) NOT NULL,
    name            VARCHAR(50)  NOT NULL,
    department      VARCHAR(100),
    phone           VARCHAR(20),
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    language        VARCHAR(10)  NOT NULL DEFAULT 'ko',
    notify_email    BOOLEAN      NOT NULL DEFAULT true,
    notify_in_app   BOOLEAN      NOT NULL DEFAULT true,
    active          BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 견적서 (파일 메타)
CREATE TABLE quotations (
    id              BIGSERIAL PRIMARY KEY,
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(10)  NOT NULL CHECK (file_type IN ('PDF','EXCEL')),
    file_key        VARCHAR(500) NOT NULL,          -- MinIO object key
    file_size       BIGINT,
    vendor          VARCHAR(200),                   -- 공급사
    product_name    VARCHAR(200),                   -- 품명
    product_number  VARCHAR(100),                   -- 품번
    eo_number       VARCHAR(100),                   -- EO NO.
    status          VARCHAR(20)  NOT NULL DEFAULT 'UPLOADED'
                    CHECK (status IN ('UPLOADED','PARSING','VERIFYING','ANALYZING','COMPLETE','FAILED')),
    progress        INTEGER      NOT NULL DEFAULT 0 CHECK (progress BETWEEN 0 AND 100),
    parsed_items    INTEGER      NOT NULL DEFAULT 0,
    anomaly_count   INTEGER      NOT NULL DEFAULT 0,
    material_cost   BIGINT       NOT NULL DEFAULT 0,
    process_cost    BIGINT       NOT NULL DEFAULT 0,
    overhead_cost   BIGINT       NOT NULL DEFAULT 0,
    total_cost      BIGINT       NOT NULL DEFAULT 0,
    department      VARCHAR(100),
    uploader_id     BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    error_message   TEXT,
    uploaded_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 파싱된 원가 항목
CREATE TABLE parsed_items (
    id              BIGSERIAL PRIMARY KEY,
    quotation_id    BIGINT       NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    category        VARCHAR(30)  NOT NULL CHECK (category IN ('material_cost','process_cost','overhead_cost','profit','other')),
    item_level      VARCHAR(5)   NOT NULL DEFAULT 'L0',
    parent_id       BIGINT       REFERENCES parsed_items(id),
    name            VARCHAR(200) NOT NULL,
    spec            VARCHAR(500),
    unit            VARCHAR(20),
    qty             DECIMAL(15,4),
    unit_price      DECIMAL(15,2),
    amount          DECIMAL(15,2),
    ratio           DECIMAL(5,2),
    confidence      DECIMAL(5,2),
    status          VARCHAR(20)  NOT NULL DEFAULT 'normal' CHECK (status IN ('normal','anomaly','corrected')),
    anomaly_reason  TEXT,
    cell_ref        VARCHAR(20),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 검증 결과
CREATE TABLE verification_results (
    id              BIGSERIAL PRIMARY KEY,
    quotation_id    BIGINT       NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    parsed_item_id  BIGINT       REFERENCES parsed_items(id) ON DELETE CASCADE,
    field_name      VARCHAR(100) NOT NULL,
    original_value  TEXT,
    parsed_value    TEXT,
    corrected_value TEXT,
    confidence      DECIMAL(5,2),
    status          VARCHAR(20)  NOT NULL DEFAULT 'correct' CHECK (status IN ('correct','warning','error')),
    message         TEXT,
    verified_by     BIGINT       REFERENCES users(id),
    verified_at     TIMESTAMPTZ
);

-- 원가 비교 세션
CREATE TABLE comparison_sessions (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200),
    product_name    VARCHAR(200),
    quotation_ids   BIGINT[]     NOT NULL,
    created_by      BIGINT       REFERENCES users(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- AI 인사이트 채팅 세션
CREATE TABLE insight_sessions (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL DEFAULT '새 대화',
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quotation_ids   BIGINT[],
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- AI 채팅 메시지
CREATE TABLE insight_messages (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT       NOT NULL REFERENCES insight_sessions(id) ON DELETE CASCADE,
    role        VARCHAR(10)  NOT NULL CHECK (role IN ('user','assistant')),
    content     TEXT         NOT NULL,
    tokens_used INTEGER,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 원가 모델 수식
CREATE TABLE cost_formulas (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    badge       VARCHAR(20)  NOT NULL DEFAULT 'sub' CHECK (badge IN ('core','sub','rate')),
    expression  TEXT         NOT NULL,
    description TEXT,
    variables   TEXT[],
    is_system   BOOLEAN      NOT NULL DEFAULT false,
    created_by  BIGINT       REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 알림
CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(50)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    message     TEXT         NOT NULL,
    is_read     BOOLEAN      NOT NULL DEFAULT false,
    ref_id      BIGINT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 활동 이력
CREATE TABLE activity_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id   BIGINT,
    detail      JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 인덱스
CREATE INDEX idx_quotations_uploader  ON quotations(uploader_id);
CREATE INDEX idx_quotations_status    ON quotations(status);
CREATE INDEX idx_quotations_uploaded  ON quotations(uploaded_at DESC);
CREATE INDEX idx_parsed_items_quot    ON parsed_items(quotation_id);
CREATE INDEX idx_verify_quot          ON verification_results(quotation_id);
CREATE INDEX idx_insight_msg_sess     ON insight_messages(session_id);
CREATE INDEX idx_notif_user_unread    ON notifications(user_id, is_read);
CREATE INDEX idx_activity_user        ON activity_logs(user_id, created_at DESC);
