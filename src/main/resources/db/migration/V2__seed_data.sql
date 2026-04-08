-- 기본 관리자 계정 (비밀번호: Admin1234! BCrypt 해시)
INSERT INTO users (employee_id, password, name, department, role)
VALUES ('ADMIN001',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        '시스템관리자', 'IT팀', 'ADMIN');

-- 기본 원가 수식
INSERT INTO cost_formulas (name, badge, expression, description, variables, is_system)
VALUES
  ('생산원가 계산식', 'core',
   '생산원가 = 재료비 + 가공비 + 제경비 + 이윤',
   '차량 부품 원가 계산의 핵심 수식으로, 모든 비용 항목의 합산값을 산출합니다.',
   ARRAY['재료비','가공비','제경비','이윤'], true),

  ('재료비 단가 계산', 'sub',
   '재료비 = 수량 × 단가 × 수율',
   '개별 재료 항목의 비용을 수량, 단가, 수율을 곱하여 산출합니다.',
   ARRAY['수량','단가','수율'], true),

  ('제경비 비율', 'rate',
   '제경비율 = 제경비 / 생산원가 × 100',
   '전체 생산원가 대비 제경비가 차지하는 비율을 백분율로 표현합니다.',
   ARRAY['제경비','생산원가'], true),

  ('가공비 단가 계산', 'sub',
   '가공비 = 직접노무비 + 간접노무비 + 설비비',
   '가공 공정에서 발생하는 세부 비용을 합산하여 총 가공비를 산출합니다.',
   ARRAY['직접노무비','간접노무비','설비비'], false);
