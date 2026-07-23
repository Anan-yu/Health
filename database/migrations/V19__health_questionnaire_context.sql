ALTER TABLE health_profile
  ADD COLUMN mood_status VARCHAR(30) NULL COMMENT '近期心情状态' AFTER stress_level,
  ADD COLUMN fear_level VARCHAR(30) NULL COMMENT '近期恐惧或焦虑感受' AFTER mood_status,
  ADD COLUMN sleep_hours DECIMAL(4,1) NULL COMMENT '近两周平均睡眠时长(小时)' AFTER sleep_quality,
  ADD COLUMN family_history VARCHAR(2000) NULL COMMENT '一级亲属疾病与发病情况' AFTER medical_history,
  ADD COLUMN recent_dietary_pattern VARCHAR(1000) NULL COMMENT '近三周食物频率与饮食结构' AFTER dietary_preference,
  ADD COLUMN diabetes_status VARCHAR(20) NULL COMMENT '糖尿病既往诊断状态' AFTER current_medications,
  ADD COLUMN hypertension_status VARCHAR(20) NULL COMMENT '高血压既往诊断状态' AFTER diabetes_status,
  ADD COLUMN dyslipidemia_status VARCHAR(20) NULL COMMENT '血脂异常既往诊断状态' AFTER hypertension_status,
  ADD COLUMN fatty_liver_status VARCHAR(20) NULL COMMENT '脂肪肝既往诊断状态' AFTER dyslipidemia_status;
