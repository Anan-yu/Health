INSERT INTO membership_plan_benefit
    (id, plan_id, benefit_code, quota_value, enabled, created_at, updated_at)
SELECT
    320000002013,
    plan.id,
    'AI_FOLLOWUP_INITIAL',
    1,
    1,
    NOW(),
    NOW()
FROM membership_plan plan
WHERE plan.plan_code = 'AI_HEALTH_YEARLY'
  AND plan.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM membership_plan_benefit existing
      WHERE existing.plan_id = plan.id
        AND existing.benefit_code = 'AI_FOLLOWUP_INITIAL'
        AND existing.deleted = 0
  );
