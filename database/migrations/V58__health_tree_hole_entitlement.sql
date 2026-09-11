-- 健康树洞使用独立会员权益：普通客户首次可体验 7 天，年度健康会员不限使用。
-- 七天窗口由 MembershipEntitlementService 根据该权益的首次有效使用时间校验。

INSERT INTO membership_benefit
    (id, benefit_code, benefit_name, description, unit_type, created_at, updated_at)
SELECT
    320000001012,
    'AI_HEALTH_TREE_HOLE',
    '健康树洞',
    '普通客户首次体验 7 天，年度健康会员期内不限使用。',
    'TRIAL_7D',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM membership_benefit
    WHERE benefit_code = 'AI_HEALTH_TREE_HOLE'
      AND deleted = 0
);

INSERT INTO membership_plan_benefit
    (id, plan_id, benefit_code, quota_value, created_at, updated_at)
SELECT
    320000002018,
    plan.id,
    'AI_HEALTH_TREE_HOLE',
    NULL,
    NOW(),
    NOW()
FROM membership_plan plan
WHERE plan.plan_code = 'FREE_CUSTOMER'
  AND plan.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM membership_plan_benefit existing
      WHERE existing.plan_id = plan.id
        AND existing.benefit_code = 'AI_HEALTH_TREE_HOLE'
        AND existing.deleted = 0
  );

INSERT INTO membership_plan_benefit
    (id, plan_id, benefit_code, quota_value, created_at, updated_at)
SELECT
    320000002019,
    plan.id,
    'AI_HEALTH_TREE_HOLE',
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
        AND existing.benefit_code = 'AI_HEALTH_TREE_HOLE'
        AND existing.deleted = 0
  );
