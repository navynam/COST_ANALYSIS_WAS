-- ============================================================
-- cost-analysis-backend  초기 데이터 (PostgreSQL)
-- ============================================================

-- ── 기본 사용자 3명 ──
-- 비밀번호: Admin1234! (BCrypt 해시)
INSERT INTO users (employee_id, password, name, department, role)
VALUES ('ADMIN001',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        '김관리', '원가관리팀', 'ADMIN')
ON CONFLICT (employee_id) DO NOTHING;

-- 비밀번호: User1234!
INSERT INTO users (employee_id, password, name, department, role)
VALUES ('USER001',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        '이분석', '견적1팀', 'USER')
ON CONFLICT (employee_id) DO NOTHING;

INSERT INTO users (employee_id, password, name, department, role)
VALUES ('USER002',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        '박검증', '견적2팀', 'USER')
ON CONFLICT (employee_id) DO NOTHING;

-- ── 기본 원가 수식 4개 ──
INSERT INTO cost_formulas (name, badge, expression, description, variables, is_system, departments)
VALUES
  ('생산원가 계산식', 'core',
   '생산원가 = 재료비 + 가공비 + 제경비 + 이윤',
   '차량 부품 원가 계산의 핵심 수식으로, 모든 비용 항목의 합산값을 산출합니다.',
   ARRAY['재료비','가공비','제경비','이윤'], true,
   '["전체"]'::jsonb),

  ('재료비 소계', 'sub',
   '재료비 = Σ(단가 × 수량 × (1 + 로스율))',
   '원자재 및 부자재의 합계를 산출합니다. 로스율을 반영합니다.',
   ARRAY['단가','수량','로스율'], true,
   '["전체"]'::jsonb),

  ('가공비 단가 계산', 'sub',
   '가공비 = 직접노무비 + 간접노무비 + 설비비',
   '가공 공정에서 발생하는 세부 비용을 합산하여 총 가공비를 산출합니다.',
   ARRAY['직접노무비','간접노무비','설비비'], false,
   '["전체"]'::jsonb),

  ('제경비율', 'rate',
   '제경비율 = 제경비 / (재료비 + 가공비) × 100',
   '제경비의 비율을 산출합니다. 일반적 범위: 8~15%',
   ARRAY['제경비','재료비','가공비'], true,
   '["전체"]'::jsonb);

-- ── 샘플 견적서 20건 (프론트엔드 mockData 동기화) ──
-- uploader_id: 1=김관리, 2=이분석, 3=박검증
INSERT INTO quotations (file_name, file_type, file_key, file_size, vendor, product_name, status, progress, parsed_items, anomaly_count, department, uploader_id)
VALUES
  ('CONSOLE_BOX_원가명세.xlsx',       'EXCEL', 'quotations/1/console_box.xlsx',     2516582,  '모비스파츠',     'CONSOLE BOX',     'PARSING',    65,  0,  0, '구매팀',       1),
  ('HEAD_LINING_원가계산서.xlsx',      'EXCEL', 'quotations/2/head_lining.xlsx',     3250585,  '대한(주)',       'HEAD LINING',     'VERIFYING',  100, 24, 2, '원가관리팀',   1),
  ('DOOR_TRIM_견적서.xlsx',            'EXCEL', 'quotations/3/door_trim.xlsx',       1887437,  '현대부품(주)',   'DOOR TRIM',       'VERIFYING',  100, 18, 0, '구매팀',       2),
  ('SEAT_COVER_원가분석.xlsx',         'EXCEL', 'quotations/4/seat_cover.xlsx',      4404019,  '현대시트',       'SEAT COVER',      'FAILED',     30,  0,  0, '품질팀',       1),
  ('BUMPER_ASSY_Q4견적.xlsx',          'EXCEL', 'quotations/5/bumper_assy.xlsx',     3040870,  '현대플라스틱',   'BUMPER ASSY',     'COMPLETE',   100, 32, 0, '원가관리팀',   1),
  ('FENDER_PANEL_원가산출.xlsx',       'EXCEL', 'quotations/6/fender_panel.xlsx',    1992294,  '한국금속(주)',   'FENDER PANEL',    'PARSING',    42,  0,  0, '견적1팀',      2),
  ('RADIATOR_GRILLE_견적서.xlsx',      'EXCEL', 'quotations/7/radiator_grille.xlsx', 2831155,  '광성산업',       'RADIATOR GRILLE', 'VERIFYING',  100, 28, 1, '견적2팀',      3),
  ('HOOD_INNER_원가계산서.xlsx',       'EXCEL', 'quotations/8/hood_inner.xlsx',      3670016,  '대한금속',       'HOOD INNER',      'VERIFYING',  100, 22, 3, '견적1팀',      2),
  ('WHEEL_COVER_견적서.xlsx',          'EXCEL', 'quotations/9/wheel_cover.xlsx',     1572864,  '한국휠(주)',     'WHEEL COVER',     'ANALYZING',  100, 15, 1, '견적2팀',      3),
  ('TRUNK_LID_원가분석.xlsx',          'EXCEL', 'quotations/10/trunk_lid.xlsx',      2936012,  '대한프레스',     'TRUNK LID',       'COMPLETE',   100, 26, 2, '구매팀',       1),
  ('SIDE_MIRROR_원가명세.xlsx',        'EXCEL', 'quotations/11/side_mirror.xlsx',    2202009,  '한국미러(주)',   'SIDE MIRROR',     'PARSING',    88,  0,  0, '원가관리팀',   1),
  ('AIR_BAG_MODULE_견적.xlsx',         'EXCEL', 'quotations/12/airbag_module.xlsx',  5033164,  '현대모비스',     'AIR BAG MODULE',  'VERIFYING',  100, 35, 4, '견적1팀',      2),
  ('CLUSTER_원가계산서.xlsx',           'EXCEL', 'quotations/13/cluster.xlsx',        2306867,  '한국전장',       'CLUSTER',         'VERIFYING',  100, 20, 0, '품질팀',       1),
  ('STEERING_WHEEL_견적.xlsx',         'EXCEL', 'quotations/14/steering_wheel.xlsx', 5347737,  '한국핸들(주)',   'STEERING WHEEL',  'FAILED',     15,  0,  0, '원가관리팀',   1),
  ('WIPER_ARM_원가분석.xlsx',          'EXCEL', 'quotations/15/wiper_arm.xlsx',      1363148,  '광성와이퍼',     'WIPER ARM',       'COMPLETE',   100, 12, 0, '구매팀',       2),
  ('ROOF_RACK_견적서.xlsx',            'EXCEL', 'quotations/16/roof_rack.xlsx',      2726297,  '한국루프',       'ROOF RACK',       'ANALYZING',  100, 19, 2, '견적2팀',      3),
  ('TAIL_LAMP_원가계산서.xlsx',        'EXCEL', 'quotations/17/tail_lamp.xlsx',      3460300,  '한국조명(주)',   'TAIL LAMP',       'PARSING',    23,  0,  0, '견적1팀',      2),
  ('EXHAUST_PIPE_견적서.xlsx',         'EXCEL', 'quotations/18/exhaust_pipe.xlsx',   1782579,  '한국배기(주)',   'EXHAUST PIPE',    'VERIFYING',  100, 14, 1, '견적2팀',      3),
  ('BRAKE_PAD_원가분석.xlsx',          'EXCEL', 'quotations/19/brake_pad.xlsx',      4089446,  '한국브레이크',   'BRAKE PAD',       'FAILED',     50,  0,  0, '구매팀',       1),
  ('SUSPENSION_ARM_견적.xlsx',         'EXCEL', 'quotations/20/suspension_arm.xlsx', 2621440,  '현대샤시(주)',   'SUSPENSION ARM',  'COMPLETE',   100, 29, 1, '원가관리팀',   1);

-- ── 샘플 변경 요청 2건 ──
INSERT INTO change_requests (formula_id, requester_id, requester_name, department, task_name, original_formula, modified_fields, status, reason, created_at)
VALUES
  (2, 2, '이분석', '견적1팀', 'HEAD_LINING 원가분석',
   '{"id":2,"name":"재료비 소계","badge":"sub","expression":"재료비 = Σ(단가 × 수량 × (1 + 로스율))","description":"원자재 및 부자재의 합계를 산출합니다. 로스율을 반영합니다.","variables":["단가","수량","로스율"]}'::jsonb,
   '{"expression":"재료비 = Σ(단가 × 수량 × (1 + 로스율) × 환율보정계수)","description":"원자재 및 부자재의 합계를 산출합니다. 로스율 및 환율 보정을 반영합니다.","variables":["단가","수량","로스율","환율보정계수"]}'::jsonb,
   'PENDING',
   'HEAD_LINING 수입 원자재에 환율 보정계수 반영이 필요합니다.',
   '2026-04-05 14:30:00+09');

INSERT INTO change_requests (formula_id, requester_id, requester_name, department, task_name, original_formula, modified_fields, status, reason, created_at, reviewed_at, reviewer_comment, approved_departments)
VALUES
  (4, 3, '박검증', '견적2팀', 'DOOR_TRIM 견적검증',
   '{"id":4,"name":"제경비율","badge":"rate","expression":"제경비율 = 제경비 / (재료비 + 가공비) × 100","description":"제경비의 비율을 산출합니다. 일반적 범위: 8~15%","variables":["제경비","재료비","가공비"]}'::jsonb,
   '{"expression":"제경비율 = (제경비 + 물류비) / (재료비 + 가공비) × 100","variables":["제경비","물류비","재료비","가공비"]}'::jsonb,
   'APPROVED',
   'DOOR_TRIM 해외 납품건 물류비를 제경비에 포함해야 합니다.',
   '2026-04-03 09:15:00+09',
   '2026-04-04 11:00:00+09',
   '물류비 포함 타당, 견적2팀에 한해 승인합니다.',
   '["견적2팀"]'::jsonb);
