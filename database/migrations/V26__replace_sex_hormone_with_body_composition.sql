-- Replace the specialist sex-hormone dimension with a broadly applicable
-- body-composition dimension driven by profile and lifestyle data.
UPDATE health_model_config
SET model_code = 'BODY_COMPOSITION',
    model_name = '体重与身体成分',
    model_category = 'BODY_COMPOSITION',
    config_snapshot = '{"engineVersion":"RULE_3.0.0","minimumIndicators":2,"contextFields":["heightCm","weightKg","bmi","waistCm","recentWeightChangeKg","exerciseFrequency"]}',
    updated_by = 10001,
    updated_at = NOW()
WHERE tenant_id = 20001
  AND model_code = 'SEX_HORMONE'
  AND status = 'ACTIVE'
  AND deleted = 0;

ALTER TABLE health_profile
  ADD COLUMN waist_cm DECIMAL(5,1) NULL COMMENT '腰围(cm)' AFTER weight_kg,
  ADD COLUMN recent_weight_change_kg DECIMAL(5,1) NULL COMMENT '近三个月体重变化(kg)' AFTER waist_cm;
