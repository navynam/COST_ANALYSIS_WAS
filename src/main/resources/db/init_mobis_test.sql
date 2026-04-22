-- ============================================================
-- mobis_test 데이터베이스 초기화 스크립트
-- 대상: PostgreSQL @ 121.134.174.251:30000 / mobis_test
-- 실행: psql -h 121.134.174.251 -p 30000 -U tobis -d mobis_test -f init_mobis_test.sql
-- ============================================================

-- 기존 테이블 제거 (재실행 안전)
DROP TABLE IF EXISTS activity_logs       CASCADE;
DROP TABLE IF EXISTS notifications        CASCADE;
DROP TABLE IF EXISTS parsing_notes        CASCADE;
DROP TABLE IF EXISTS change_requests      CASCADE;
DROP TABLE IF EXISTS cost_formulas        CASCADE;
DROP TABLE IF EXISTS insight_messages     CASCADE;
DROP TABLE IF EXISTS insight_sessions     CASCADE;
DROP TABLE IF EXISTS comparison_sessions  CASCADE;
DROP TABLE IF EXISTS verification_results CASCADE;
DROP TABLE IF EXISTS parsed_items         CASCADE;
DROP TABLE IF EXISTS quotations           CASCADE;
DROP TABLE IF EXISTS users                CASCADE;

-- ============================================================
-- 1. 스키마 생성
-- ============================================================

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

CREATE TABLE quotations (
    id              BIGSERIAL PRIMARY KEY,
    file_name       VARCHAR(255) NOT NULL,
    file_type       VARCHAR(10)  NOT NULL CHECK (file_type IN ('PDF','EXCEL')),
    file_key        VARCHAR(500) NOT NULL,
    file_size       BIGINT,
    vendor          VARCHAR(200),
    product_name    VARCHAR(200),
    product_number  VARCHAR(100),
    eo_number       VARCHAR(100),
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

CREATE TABLE comparison_sessions (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200),
    product_name    VARCHAR(200),
    quotation_ids   BIGINT[]     NOT NULL,
    created_by      BIGINT       REFERENCES users(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE insight_sessions (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL DEFAULT '새 대화',
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    quotation_ids   BIGINT[],
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE insight_messages (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT       NOT NULL REFERENCES insight_sessions(id) ON DELETE CASCADE,
    role        VARCHAR(10)  NOT NULL CHECK (role IN ('user','assistant')),
    content     TEXT         NOT NULL,
    tokens_used INTEGER,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE cost_formulas (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    badge       VARCHAR(20)  NOT NULL DEFAULT 'sub' CHECK (badge IN ('core','sub','rate')),
    expression  TEXT         NOT NULL,
    description TEXT,
    variables   TEXT[],
    departments JSONB        DEFAULT '[]'::jsonb,
    is_system   BOOLEAN      NOT NULL DEFAULT false,
    created_by  BIGINT       REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE change_requests (
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

CREATE TABLE parsing_notes (
    id          BIGSERIAL PRIMARY KEY,
    file_id     BIGINT       NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(30)  NOT NULL DEFAULT 'parsing'
                CHECK (type IN ('parsing','upload','format','improvement')),
    content     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

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
CREATE INDEX idx_quotations_uploader     ON quotations(uploader_id);
CREATE INDEX idx_quotations_status       ON quotations(status);
CREATE INDEX idx_quotations_uploaded     ON quotations(uploaded_at DESC);
CREATE INDEX idx_parsed_items_quot       ON parsed_items(quotation_id);
CREATE INDEX idx_verify_quot             ON verification_results(quotation_id);
CREATE INDEX idx_insight_msg_sess        ON insight_messages(session_id);
CREATE INDEX idx_notif_user_unread       ON notifications(user_id, is_read);
CREATE INDEX idx_activity_user           ON activity_logs(user_id, created_at DESC);
CREATE INDEX idx_change_req_formula      ON change_requests(formula_id);
CREATE INDEX idx_change_req_status       ON change_requests(status);
CREATE INDEX idx_change_req_requester    ON change_requests(requester_id);
CREATE INDEX idx_parsing_notes_file      ON parsing_notes(file_id);
CREATE INDEX idx_parsing_notes_user      ON parsing_notes(user_id);

-- ============================================================
-- 2. 사용자 목업 (비밀번호: Admin1234! BCrypt 해시 공통)
-- ============================================================
INSERT INTO users (employee_id, password, name, department, phone, role) VALUES
  ('ADMIN001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '시스템관리자', 'IT팀',    '010-1000-0001', 'ADMIN'),
  ('USER001',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '이분석',      '견적1팀', '010-1000-0002', 'USER'),
  ('USER002',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '박검증',      '견적2팀', '010-1000-0003', 'USER'),
  ('USER003',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '김원가',      '견적1팀', '010-1000-0004', 'USER'),
  ('USER004',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '최모비스',    '구매팀',  '010-1000-0005', 'USER');

-- ============================================================
-- 3. 원가 수식 목업
-- ============================================================
INSERT INTO cost_formulas (name, badge, expression, description, variables, departments, is_system) VALUES
  ('생산원가 계산식', 'core',
   '생산원가 = 재료비 + 가공비 + 제경비 + 이윤',
   '차량 부품 원가 계산의 핵심 수식으로, 모든 비용 항목의 합산값을 산출합니다.',
   ARRAY['재료비','가공비','제경비','이윤'], '["전체"]'::jsonb, true),
  ('재료비 소계', 'sub',
   '재료비 = Σ(단가 × 수량 × (1 + 로스율))',
   '원자재 및 부자재의 합계를 산출합니다. 로스율을 반영합니다.',
   ARRAY['단가','수량','로스율'], '["견적1팀","견적2팀"]'::jsonb, true),
  ('제경비율', 'rate',
   '제경비율 = 제경비 / (재료비 + 가공비) × 100',
   '제경비의 비율을 산출합니다. 일반적 범위: 8~15%',
   ARRAY['제경비','재료비','가공비'], '["전체"]'::jsonb, true),
  ('가공비 단가 계산', 'sub',
   '가공비 = 직접노무비 + 간접노무비 + 설비비',
   '가공 공정에서 발생하는 세부 비용을 합산하여 총 가공비를 산출합니다.',
   ARRAY['직접노무비','간접노무비','설비비'], '["견적1팀"]'::jsonb, false);

-- ============================================================
-- 4. 견적서 목업 (프론트 mockData.ts 기반 20건)
-- ============================================================
INSERT INTO quotations (file_name, file_type, file_key, file_size, vendor, product_name, product_number, eo_number,
                        status, progress, parsed_items, anomaly_count,
                        material_cost, process_cost, overhead_cost, total_cost,
                        department, uploader_id, uploaded_at) VALUES
  ('HEAD_LINING_원가계산서.xlsx',   'EXCEL', 'mock/hl-001.xlsx',  125000, '대한(주)',       'HEAD LINING ASSY',     'HL-2024-001', 'EO-2024-1201', 'VERIFYING',  60,  24,  2,  45000000, 18000000, 12000000, 75000000, '견적1팀', 2, now() - interval '10 minutes'),
  ('DOOR_TRIM_견적서.xlsx',         'EXCEL', 'mock/dt-001.xlsx',  148000, '현대부품(주)',   'DOOR TRIM ASSY',        'DT-2024-012', 'EO-2024-1015', 'COMPLETE',  100,  38,  0,  52000000, 22000000, 14000000, 88000000, '견적1팀', 2, now() - interval '1 hour'),
  ('BUMPER_ASSY_Q4견적.xlsx',       'EXCEL', 'mock/bp-001.xlsx',  210000, '현대플라스틱',   'FRONT BUMPER',          'BP-2024-033', 'EO-2024-0902', 'COMPLETE',  100,  42,  1,  68000000, 30000000, 18000000, 116000000, '견적2팀', 3, now() - interval '1 day'),
  ('SEAT_COVER_원가분석.xlsx',      'EXCEL', 'mock/sc-001.xlsx',  88000,  '현대시트',       'SEAT COVER',            'SC-2024-008', 'EO-2024-0820', 'FAILED',     35,   0,  0,  0,        0,        0,        0,        '견적1팀', 3, now() - interval '3 days'),
  ('CONSOLE_BOX_원가명세.xlsx',     'EXCEL', 'mock/cb-001.xlsx',  115000, '모비스파츠',     'CONSOLE BOX',           'CB-2024-019', 'EO-2024-0711', 'PARSING',    45,   0,  0,  0,        0,        0,        0,        '구매팀',  5, now() - interval '30 minutes'),
  ('CRASH_PAD_견적서.xlsx',         'EXCEL', 'mock/cp-001.xlsx',  175000, '대한(주)',       'CRASH PAD ASSY',        'CP-2024-002', 'EO-2024-1118', 'VERIFYING',  80,  30,  1,  58000000, 25000000, 15000000, 98000000, '견적1팀', 2, now() - interval '2 hours'),
  ('PILLAR_TRIM_원가계산.xlsx',     'EXCEL', 'mock/pt-001.xlsx',  96000,  '현대부품(주)',   'PILLAR TRIM',           'PT-2024-045', 'EO-2024-0605', 'COMPLETE',  100,  22,  0,  28000000, 12000000, 7000000,  47000000, '견적2팀', 3, now() - interval '5 days'),
  ('SUNVISOR_ASSY_견적.xlsx',       'EXCEL', 'mock/sv-001.xlsx',  62000,  '현대시트',       'SUNVISOR ASSY',         'SV-2024-003', 'EO-2024-0510', 'ANALYZING', 85,  18,  0,  15000000, 8000000,  5000000,  28000000, '견적1팀', 4, now() - interval '3 hours'),
  ('GLOVE_BOX_원가분석.xlsx',       'EXCEL', 'mock/gb-001.xlsx',  108000, '대한(주)',       'GLOVE BOX',             'GB-2024-021', 'EO-2024-0402', 'COMPLETE',  100,  26,  0,  32000000, 15000000, 9000000,  56000000, '견적2팀', 2, now() - interval '1 week'),
  ('ARM_REST_견적서.xlsx',          'EXCEL', 'mock/ar-001.xlsx',  72000,  '모비스파츠',     'ARM REST',              'AR-2024-015', 'EO-2024-0325', 'UPLOADED',  10,   0,  0,  0,        0,        0,        0,        '구매팀',  5, now() - interval '15 minutes'),
  ('PACKAGE_TRAY_원가.xlsx',        'EXCEL', 'mock/pg-001.xlsx',  92000,  '현대부품(주)',   'PACKAGE TRAY',          'PG-2024-007', 'EO-2024-0228', 'VERIFYING',  75,  20,  2,  22000000, 10000000, 6000000,  38000000, '견적1팀', 3, now() - interval '4 hours'),
  ('DASH_BOARD_견적서.xlsx',        'EXCEL', 'mock/db-001.xlsx',  198000, '현대플라스틱',   'DASH BOARD',            'DB-2024-011', 'EO-2024-0201', 'COMPLETE',  100,  48,  0,  75000000, 35000000, 22000000, 132000000,'견적2팀', 3, now() - interval '2 weeks'),
  ('FLOOR_CARPET_원가.xlsx',        'EXCEL', 'mock/fc-001.xlsx',  135000, '현대시트',       'FLOOR CARPET',          'FC-2024-009', 'EO-2024-0120', 'PARSING',    25,   0,  0,  0,        0,        0,        0,        '견적1팀', 4, now() - interval '20 minutes'),
  ('RADIATOR_GRILLE_견적.xlsx',     'EXCEL', 'mock/rg-001.xlsx',  85000,  '대한(주)',       'RADIATOR GRILLE',       'RG-2024-024', 'EO-2024-0105', 'ANALYZING', 90,  28,  0,  18000000, 9000000,  5500000,  32500000, '견적2팀', 2, now() - interval '6 hours'),
  ('MIRROR_ASSY_원가계산.xlsx',     'EXCEL', 'mock/mr-001.xlsx',  78000,  '모비스파츠',     'OUT SIDE MIRROR',       'MR-2024-013', 'EO-2023-1220', 'COMPLETE',  100,  19,  0,  21000000, 11000000, 7000000,  39000000, '구매팀',  5, now() - interval '3 weeks'),
  ('FENDER_LINER_견적서.xlsx',      'EXCEL', 'mock/fl-001.xlsx',  102000, '현대플라스틱',   'FENDER LINER',          'FL-2024-030', 'EO-2023-1130', 'FAILED',     50,   0,  0,  0,        0,        0,        0,        '견적1팀', 3, now() - interval '5 days'),
  ('HOOD_INSULATOR_원가.xlsx',      'EXCEL', 'mock/hi-001.xlsx',  67000,  '현대부품(주)',   'HOOD INSULATOR',        'HI-2024-006', 'EO-2023-1101', 'VERIFYING',  70,  16,  1,  12000000, 6000000,  3500000,  21500000, '견적2팀', 2, now() - interval '7 hours'),
  ('TAIL_GATE_TRIM_견적.xlsx',      'EXCEL', 'mock/tg-001.xlsx',  118000, '대한(주)',       'TAIL GATE TRIM',        'TG-2024-017', 'EO-2023-1015', 'COMPLETE',  100,  32,  0,  38000000, 18000000, 11000000, 67000000, '견적1팀', 4, now() - interval '4 weeks'),
  ('WHEEL_HOUSE_원가분석.xlsx',     'EXCEL', 'mock/wh-001.xlsx',  95000,  '모비스파츠',     'WHEEL HOUSE',           'WH-2024-026', 'EO-2023-0920', 'ANALYZING', 60,  24,  0,  24000000, 12000000, 7000000,  43000000, '구매팀',  5, now() - interval '8 hours'),
  ('COWL_TOP_COVER_견적.xlsx',      'EXCEL', 'mock/ct-001.xlsx',  73000,  '현대시트',       'COWL TOP COVER',        'CT-2024-018', 'EO-2023-0901', 'UPLOADED',   5,   0,  0,  0,        0,        0,        0,        '견적2팀', 3, now() - interval '5 minutes');

-- ============================================================
-- 5. 파싱 항목 목업 (HEAD_LINING 기준 샘플)
-- ============================================================
INSERT INTO parsed_items (quotation_id, category, item_level, name, spec, unit, qty, unit_price, amount, confidence, status, cell_ref) VALUES
  (1, 'material_cost', 'L0', '인서트필름',     '100x50x0.5',    'EA', 10, 12.05, 120.50, 98.5, 'normal',  'C15'),
  (1, 'material_cost', 'L0', 'PAD-ANTINOISE',  '80x40x2',       'EA',  5, 17.00, 85.00,  97.2, 'normal',  'C16'),
  (1, 'material_cost', 'L0', 'TAPPING-SCREW',  'M5x20',         'EA',  8,  5.65, 45.20,  85.3, 'anomaly', 'C17'),
  (1, 'material_cost', 'L0', 'FASTENER CLIP',  'TYPE-A',        'EA',  4,  8.20, 32.80,  94.0, 'normal',  'C18'),
  (1, 'process_cost',  'L0', '사출성형',       'P01',           'CT', 45, 55.56, 2500.00, 96.5, 'normal', 'C20'),
  (1, 'process_cost',  'L0', '조립',           'P02',           'CT', 30, 40.00, 1200.00, 95.8, 'normal', 'C21'),
  (1, 'process_cost',  'L0', '검사',           'P03',           'CT', 20, 40.00,  800.00, 88.1, 'anomaly','C22'),
  (1, 'overhead_cost', 'L0', 'HEAD_LINING 경비1', '',            'CT', 45,333.33,15000.00, 97.0, 'normal', 'C25'),
  (1, 'overhead_cost', 'L0', 'HEAD_LINING 경비2', '',            'CT', 30,283.33, 8500.00, 96.3, 'normal', 'C26');

-- ============================================================
-- 6. 검증 결과 목업
-- ============================================================
INSERT INTO verification_results (quotation_id, parsed_item_id, field_name, original_value, parsed_value, confidence, status, message, verified_by, verified_at) VALUES
  (1, 3, '단가',     '5.65', '5.65',  85.3, 'warning', '시장가 대비 12% 초과, 검토 필요',      2, now() - interval '5 minutes'),
  (1, 7, '적용CT',   '20',   '20',    88.1, 'warning', '평균 대비 CT 짧음, 재확인 필요',        2, now() - interval '5 minutes'),
  (2, NULL, '전체',  '',     '',      98.7, 'correct', '정상 파싱 완료',                        3, now() - interval '50 minutes');

-- ============================================================
-- 7. 변경 요청 목업
-- ============================================================
INSERT INTO change_requests (formula_id, requester_id, requester_name, department, task_name,
                             original_formula, modified_fields, status, reason, created_at) VALUES
  (2, 2, '이분석', '견적1팀', 'HEAD_LINING 원가분석',
   '{"id":2,"name":"재료비 소계","badge":"sub","expression":"재료비 = Σ(단가 × 수량 × (1 + 로스율))","variables":["단가","수량","로스율"]}'::jsonb,
   '{"expression":"재료비 = Σ(단가 × 수량 × (1 + 로스율) × 환율보정계수)","variables":["단가","수량","로스율","환율보정계수"]}'::jsonb,
   'PENDING', 'HEAD_LINING 수입 원자재에 환율 보정계수 반영이 필요합니다.', now() - interval '2 days');

INSERT INTO change_requests (formula_id, requester_id, requester_name, department, task_name,
                             original_formula, modified_fields, status, reason, created_at,
                             reviewed_at, reviewer_comment, approved_departments) VALUES
  (3, 3, '박검증', '견적2팀', 'DOOR_TRIM 견적검증',
   '{"id":3,"name":"제경비율","badge":"rate","expression":"제경비율 = 제경비 / (재료비 + 가공비) × 100","variables":["제경비","재료비","가공비"]}'::jsonb,
   '{"expression":"제경비율 = (제경비 + 물류비) / (재료비 + 가공비) × 100","variables":["제경비","물류비","재료비","가공비"]}'::jsonb,
   'APPROVED', 'DOOR_TRIM 해외 납품건 물류비를 제경비에 포함해야 합니다.',
   now() - interval '4 days', now() - interval '3 days',
   '물류비 포함 타당, 견적2팀에 한해 승인합니다.', '["견적2팀"]'::jsonb);

-- ============================================================
-- 8. 파싱 노트 목업
-- ============================================================
INSERT INTO parsing_notes (file_id, user_id, type, content) VALUES
  (1, 2, 'parsing',     'HEAD_LINING — C17 셀의 TAPPING-SCREW 단가가 시장가 대비 높음, 재확인 필요'),
  (1, 2, 'improvement', '재료비 항목 자동 분류 로직에 로스율 기본값 2% 적용 검토'),
  (4, 3, 'format',      'SEAT_COVER — 템플릿 형식이 표준과 달라 파싱 실패, 표준 포맷 요청 필요');

-- ============================================================
-- 9. 알림 목업
-- ============================================================
INSERT INTO notifications (user_id, type, title, message, is_read, ref_id) VALUES
  (2, 'ANOMALY', '이상치 감지',           'HEAD_LINING 견적서에서 이상치 2건이 감지되었습니다.',   false, 1),
  (2, 'READY',   '검증 완료',             'DOOR_TRIM 견적서 검증이 완료되었습니다.',              false, 2),
  (3, 'READY',   '분석 완료',             'BUMPER_ASSY Q4 견적 분석이 완료되었습니다.',           true,  3),
  (3, 'FAILED',  '파싱 실패',             'SEAT_COVER 원가분석 파일 파싱에 실패했습니다.',        false, 4),
  (5, 'PROGRESS','파싱 진행중',           'CONSOLE_BOX 원가명세 파싱이 진행 중입니다.',          false, 5);

-- ============================================================
-- 10. 활동 로그 목업
-- ============================================================
INSERT INTO activity_logs (user_id, action, target_type, target_id, detail, ip_address) VALUES
  (2, 'UPLOAD_QUOTATION', 'QUOTATION', 1,  '{"fileName":"HEAD_LINING_원가계산서.xlsx"}'::jsonb, '10.0.0.12'),
  (2, 'VERIFY_QUOTATION', 'QUOTATION', 2,  '{"fileName":"DOOR_TRIM_견적서.xlsx"}'::jsonb,       '10.0.0.12'),
  (3, 'ANALYZE_QUOTATION','QUOTATION', 3,  '{"fileName":"BUMPER_ASSY_Q4견적.xlsx"}'::jsonb,     '10.0.0.13'),
  (2, 'REQUEST_CHANGE',   'FORMULA',   2,  '{"formulaName":"재료비 소계"}'::jsonb,              '10.0.0.12'),
  (1, 'APPROVE_CHANGE',   'CHANGE_REQ',2,  '{"status":"APPROVED"}'::jsonb,                      '10.0.0.10');

-- ============================================================
-- 완료
-- ============================================================
SELECT 'mobis_test 초기화 완료' AS result,
       (SELECT count(*) FROM users)         AS users,
       (SELECT count(*) FROM quotations)    AS quotations,
       (SELECT count(*) FROM parsed_items)  AS parsed_items,
       (SELECT count(*) FROM cost_formulas) AS formulas,
       (SELECT count(*) FROM change_requests) AS change_reqs,
       (SELECT count(*) FROM notifications) AS notifications;
