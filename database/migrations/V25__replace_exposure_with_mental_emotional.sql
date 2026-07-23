-- Replace the low-frequency heavy-metal dimension with a questionnaire-driven
-- psychological and emotional health dimension.
UPDATE health_model_config
SET model_code = 'MENTAL_EMOTIONAL',
    model_name = '心理与情绪健康',
    model_category = 'MENTAL_HEALTH',
    config_snapshot = '{"engineVersion":"RULE_3.0.0","minimumIndicators":2,"contextFields":["stressLevel","moodStatus","fearLevel"]}',
    updated_by = 10001,
    updated_at = NOW()
WHERE tenant_id = 20001
  AND model_code = 'HEAVY_METAL_EXPOSURE'
  AND status = 'ACTIVE'
  AND deleted = 0;
