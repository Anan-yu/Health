UPDATE membership_benefit
SET benefit_name = '健康助手',
    updated_at = NOW()
WHERE benefit_code = 'AI_MEDICAL_ASSISTANT'
  AND deleted = 0
  AND benefit_name = '三羊健康助手';
