ALTER TABLE health_profile
  ADD COLUMN bmi DECIMAL(5,2) NULL COMMENT 'BMI指数' AFTER weight_kg,
  ADD COLUMN medical_history VARCHAR(2000) NULL COMMENT '既往病史' AFTER lifestyle_summary,
  ADD COLUMN allergy_history VARCHAR(1000) NULL COMMENT '过敏史' AFTER medical_history,
  ADD COLUMN current_medications VARCHAR(1000) NULL COMMENT '当前用药' AFTER allergy_history,
  ADD COLUMN smoking_status VARCHAR(20) NULL COMMENT '吸烟状态' AFTER current_medications,
  ADD COLUMN alcohol_status VARCHAR(20) NULL COMMENT '饮酒状态' AFTER smoking_status,
  ADD COLUMN exercise_frequency VARCHAR(50) NULL COMMENT '运动频率' AFTER alcohol_status,
  ADD COLUMN sleep_quality VARCHAR(50) NULL COMMENT '睡眠质量' AFTER exercise_frequency,
  ADD COLUMN stress_level VARCHAR(50) NULL COMMENT '压力水平' AFTER sleep_quality,
  ADD COLUMN dietary_preference VARCHAR(200) NULL COMMENT '饮食偏好' AFTER stress_level;
