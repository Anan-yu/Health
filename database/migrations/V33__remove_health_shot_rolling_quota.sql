-- 健康拍仅保留每日额度；历史滚动额度流水仍保留，便于审计和历史使用记录展示。
UPDATE membership_plan_benefit
SET enabled = 0,
    deleted = 1,
    updated_at = NOW()
WHERE benefit_code = 'HEALTH_SHOT_30D'
  AND deleted = 0;
