-- 按产品权益表统一免费客户与年度健康会员的实际额度。
-- 不修改已执行迁移，保留历史 membership_usage 流水。

UPDATE membership_plan_benefit
SET quota_value = 3,
    updated_by = 0,
    updated_at = NOW()
WHERE plan_id = (SELECT id FROM membership_plan WHERE plan_code = 'FREE_CUSTOMER' AND deleted = 0)
  AND benefit_code IN (
      'AI_HEALTH_REPORT',
      'HEALTH_SHOT',
      'TTS_MEAL_REMINDER',
      'TTS_SLEEP_REMINDER'
  )
  AND deleted = 0;

UPDATE membership_plan_benefit
SET quota_value = NULL,
    updated_by = 0,
    updated_at = NOW()
WHERE plan_id = (SELECT id FROM membership_plan WHERE plan_code = 'AI_HEALTH_YEARLY' AND deleted = 0)
  AND benefit_code = 'AI_HEALTH_REPORT'
  AND deleted = 0;

-- 年度会员的首次随访仍保留首次生成语义；持续随访使用 ENABLED 权益。
UPDATE membership_plan_benefit
SET quota_value = 1,
    updated_by = 0,
    updated_at = NOW()
WHERE plan_id = (SELECT id FROM membership_plan WHERE plan_code = 'AI_HEALTH_YEARLY' AND deleted = 0)
  AND benefit_code = 'AI_FOLLOWUP_INITIAL'
  AND deleted = 0;
