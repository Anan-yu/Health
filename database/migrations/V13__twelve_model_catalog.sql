-- Upgrade the development tenant from the five-model demo catalog to the versioned
-- twelve-model health-management catalog. Python remains the executable rule source;
-- this table controls tenant activation and exposes model metadata to administrators.
UPDATE health_model_config
SET status = 'DEPRECATED', updated_by = 10001, updated_at = NOW()
WHERE tenant_id = 20001 AND status = 'ACTIVE';

INSERT INTO health_model_config
  (id, tenant_id, model_code, model_name, model_category, version, status,
   config_snapshot, created_by, created_at, updated_by, updated_at, deleted,
   optimistic_version)
VALUES
  (80101, 20001, 'GLUCOSE_METABOLISM', '糖代谢与胰岛素抵抗', 'METABOLISM', '3.0.0', 'ACTIVE',
   '{"engineVersion":"RULE_3.0.0","minimumIndicators":2,"indicators":["fasting_glucose","fasting_insulin","hba1c","triglyceride","hdl"]}',
   10001, NOW(), 10001, NOW(), 0, 0),
  (80102, 20001, 'LIPID_CARDIOVASCULAR', '血脂与心血管代谢', 'CARDIOVASCULAR', '3.0.0', 'ACTIVE',
   '{"engineVersion":"RULE_3.0.0","minimumIndicators":2,"indicators":["total_cholesterol","ldl","hdl","triglyceride","apob","lpa"]}',
   10001, NOW(), 10001, NOW(), 0, 0),
  (80103, 20001, 'CHRONIC_INFLAMMATION', '慢性炎症与免疫负荷', 'INFLAMMATION', '3.0.0', 'ACTIVE',
   '{"engineVersion":"RULE_3.0.0","minimumIndicators":2,"indicators":["crp","hs_crp","esr","homocysteine","ferritin","wbc"]}',
   10001, NOW(), 10001, NOW(), 0, 0),
  (80104, 20001, 'LIVER_METABOLIC', '肝脏与代谢负担', 'METABOLISM', '3.0.0', 'ACTIVE',
   '{"engineVersion":"RULE_3.0.0","minimumIndicators":2,"indicators":["alt","ast","ggt","total_bilirubin","albumin"]}',
   10001, NOW(), 10001, NOW(), 0, 0),
  (80105, 20001, 'KIDNEY_ELECTROLYTE', '肾功能与电解质', 'RENAL', '3.0.0', 'ACTIVE',
   '{"engineVersion":"RULE_3.0.0","minimumIndicators":2,"indicators":["creatinine","egfr","urea","uric_acid","sodium","potassium"]}',
   10001, NOW(), 10001, NOW(), 0, 0),
  (80106, 20001, 'HEMATOLOGY_ANEMIA', '血液与贫血相关风险', 'HEMATOLOGY', '3.0.0', 'ACTIVE',
   '{"engineVersion":"RULE_3.0.0","minimumIndicators":3,"indicators":["hemoglobin","rbc","mcv","mch","ferritin","vitamin_b12","folate"]}',
   10001, NOW(), 10001, NOW(), 0, 0),
  (80107, 20001, 'THYROID_HORMONE', '甲状腺功能', 'ENDOCRINE', '3.0.0', 'ACTIVE',
   '{"engineVersion":"RULE_3.0.0","minimumIndicators":2,"indicators":["tsh","ft3","ft4","tpo_ab","tg_ab"]}',
   10001, NOW(), 10001, NOW(), 0, 0),
  (80108, 20001, 'SEX_HORMONE', '性激素与生殖内分泌', 'ENDOCRINE', '3.0.0', 'ACTIVE',
   '{"engineVersion":"RULE_3.0.0","minimumIndicators":3,"requires":["gender","age"],"indicators":["estradiol","progesterone","testosterone","shbg","lh","fsh"]}',
   10001, NOW(), 10001, NOW(), 0, 0),
  (80109, 20001, 'HPA_ADRENAL', 'HPA压力、睡眠与恢复', 'STRESS_RECOVERY', '3.0.0', 'ACTIVE',
   '{"engineVersion":"RULE_3.0.0","minimumIndicators":2,"indicators":["cortisol_am","cortisol_pm","dhea_s","sleep_hours","hrv"]}',
   10001, NOW(), 10001, NOW(), 0, 0),
  (80110, 20001, 'NUTRITION_MICRONUTRIENT', '营养与微量元素', 'NUTRITION', '3.0.0', 'ACTIVE',
   '{"engineVersion":"RULE_3.0.0","minimumIndicators":2,"indicators":["vitamin_d","vitamin_b12","folate","ferritin","zinc","magnesium","calcium"]}',
   10001, NOW(), 10001, NOW(), 0, 0),
  (80111, 20001, 'GUT_BARRIER', '消化与肠道屏障', 'GUT', '3.0.0', 'ACTIVE',
   '{"engineVersion":"RULE_3.0.0","minimumIndicators":2,"indicators":["zonulin","calprotectin","occult_blood","stool_ph","digestive_symptom_score"]}',
   10001, NOW(), 10001, NOW(), 0, 0),
  (80112, 20001, 'HEAVY_METAL_EXPOSURE', '重金属与环境暴露', 'EXPOSURE', '3.0.0', 'ACTIVE',
   '{"engineVersion":"RULE_3.0.0","minimumIndicators":1,"indicators":["blood_lead","blood_mercury","cadmium","arsenic","heavy_metal_panel"]}',
   10001, NOW(), 10001, NOW(), 0, 0);
