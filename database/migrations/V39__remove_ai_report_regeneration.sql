-- AI 报告重新解读不再作为客户会员权益提供。
-- 保留历史 membership_usage 记录，避免破坏已发生的权益流水和审计数据。
UPDATE membership_plan_benefit
SET enabled = 0,
    deleted = 1,
    updated_by = 0,
    updated_at = NOW()
WHERE benefit_code = 'AI_REPORT_REGENERATE'
  AND deleted = 0;

UPDATE membership_benefit
SET status = 'DISABLED',
    deleted = 1,
    updated_by = 0,
    updated_at = NOW()
WHERE benefit_code = 'AI_REPORT_REGENERATE'
  AND deleted = 0;
