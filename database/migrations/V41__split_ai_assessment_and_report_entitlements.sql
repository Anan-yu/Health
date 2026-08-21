-- 将 AI 健康评估与 AI 健康报告拆为两个独立权益。
-- 旧的 AI_HEALTH_REPORT 使用流水保留并继续计入报告额度；后端会将历史综合评估流水同时视为评估额度消耗。

UPDATE membership_benefit
SET benefit_name = 'AI 健康报告',
    description = '生成综合健康报告。',
    updated_by = 0,
    updated_at = NOW()
WHERE benefit_code = 'AI_HEALTH_REPORT'
  AND deleted = 0;

INSERT INTO membership_benefit
    (id, benefit_code, benefit_name, description, unit_type, created_at, updated_at)
SELECT
    320000001010,
    'AI_HEALTH_ASSESSMENT',
    'AI 健康评估',
    '生成多维健康评估。',
    'YEARLY',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM membership_benefit
    WHERE benefit_code = 'AI_HEALTH_ASSESSMENT'
      AND deleted = 0
);

INSERT INTO membership_plan_benefit
    (id, plan_id, benefit_code, quota_value, created_at, updated_at)
SELECT
    320000002014,
    plan.id,
    'AI_HEALTH_ASSESSMENT',
    3,
    NOW(),
    NOW()
FROM membership_plan plan
WHERE plan.plan_code = 'FREE_CUSTOMER'
  AND plan.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM membership_plan_benefit existing
      WHERE existing.plan_id = plan.id
        AND existing.benefit_code = 'AI_HEALTH_ASSESSMENT'
        AND existing.deleted = 0
  );

INSERT INTO membership_plan_benefit
    (id, plan_id, benefit_code, quota_value, created_at, updated_at)
SELECT
    320000002015,
    plan.id,
    'AI_HEALTH_ASSESSMENT',
    NULL,
    NOW(),
    NOW()
FROM membership_plan plan
WHERE plan.plan_code = 'AI_HEALTH_YEARLY'
  AND plan.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM membership_plan_benefit existing
      WHERE existing.plan_id = plan.id
        AND existing.benefit_code = 'AI_HEALTH_ASSESSMENT'
        AND existing.deleted = 0
  );
