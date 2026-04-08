-- ============================================================
-- V4: 추가 사용자 + 변경 요청 시드 데이터
-- ============================================================

-- 추가 사용자 2명 (V2에서 관리자 1명만 있었음)
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

-- 기존 수식에 departments 업데이트
UPDATE cost_formulas SET departments = '["전체"]'::jsonb WHERE departments IS NULL OR departments = '[]'::jsonb;

-- 변경 요청 시드 데이터
INSERT INTO change_requests (formula_id, requester_id, requester_name, department, task_name,
                             original_formula, modified_fields, status, reason, created_at)
SELECT 2, u.id, '이분석', '견적1팀', 'HEAD_LINING 원가분석',
       '{"id":2,"name":"재료비 소계","badge":"sub","expression":"재료비 = Σ(단가 × 수량 × (1 + 로스율))","description":"원자재 및 부자재의 합계를 산출합니다. 로스율을 반영합니다.","variables":["단가","수량","로스율"]}'::jsonb,
       '{"expression":"재료비 = Σ(단가 × 수량 × (1 + 로스율) × 환율보정계수)","description":"원자재 및 부자재의 합계를 산출합니다. 로스율 및 환율 보정을 반영합니다.","variables":["단가","수량","로스율","환율보정계수"]}'::jsonb,
       'PENDING',
       'HEAD_LINING 수입 원자재에 환율 보정계수 반영이 필요합니다.',
       '2026-04-05 14:30:00+09'
FROM users u WHERE u.employee_id = 'USER001';

INSERT INTO change_requests (formula_id, requester_id, requester_name, department, task_name,
                             original_formula, modified_fields, status, reason, created_at,
                             reviewed_at, reviewer_comment, approved_departments)
SELECT 4, u.id, '박검증', '견적2팀', 'DOOR_TRIM 견적검증',
       '{"id":4,"name":"제경비율","badge":"rate","expression":"제경비율 = 제경비 / (재료비 + 가공비) × 100","description":"제경비의 비율을 산출합니다. 일반적 범위: 8~15%","variables":["제경비","재료비","가공비"]}'::jsonb,
       '{"expression":"제경비율 = (제경비 + 물류비) / (재료비 + 가공비) × 100","variables":["제경비","물류비","재료비","가공비"]}'::jsonb,
       'APPROVED',
       'DOOR_TRIM 해외 납품건 물류비를 제경비에 포함해야 합니다.',
       '2026-04-03 09:15:00+09',
       '2026-04-04 11:00:00+09',
       '물류비 포함 타당, 견적2팀에 한해 승인합니다.',
       '["견적2팀"]'::jsonb
FROM users u WHERE u.employee_id = 'USER002';
