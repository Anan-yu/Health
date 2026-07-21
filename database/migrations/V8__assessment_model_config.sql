CREATE TABLE health_model_config (
  id BIGINT NOT NULL,
  tenant_id BIGINT NOT NULL,
  model_code VARCHAR(80) NOT NULL,
  model_name VARCHAR(100) NOT NULL,
  model_category VARCHAR(50) NOT NULL,
  version VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  config_snapshot JSON NOT NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_by BIGINT NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  optimistic_version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_model_version (tenant_id, model_code, version),
  KEY idx_model_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估模型配置表';

-- Seed 5 demo models for tenant 20001
INSERT INTO health_model_config (id, tenant_id, model_code, model_name, model_category, version, status, config_snapshot, created_by, created_at, updated_by, updated_at, deleted, optimistic_version) VALUES
(80001, 20001, 'GLUCOSE_METABOLISM', '糖代谢失衡评估', 'METABOLISM', '1.0.0', 'ACTIVE', '{"indicators":["fasting_glucose","fasting_insulin","hba1c","triglyceride","hdl"],"rules":[{"indicatorCode":"fasting_glucose","condition":"GT","threshold":6.1,"riskLevel":"ATTENTION","evidence":"空腹血糖高于参考上限"},{"indicatorCode":"hba1c","condition":"GT","threshold":6.0,"riskLevel":"ATTENTION","evidence":"糖化血红蛋白偏高"}]}', 10001, NOW(), 10001, NOW(), 0, 0),
(80002, 20001, 'CHRONIC_INFLAMMATION', '慢性炎症与血管老化', 'INFLAMMATION', '1.0.0', 'ACTIVE', '{"indicators":["crp","homocysteine","ferritin"],"rules":[{"indicatorCode":"crp","condition":"GT","threshold":3.0,"riskLevel":"ATTENTION","evidence":"C反应蛋白高于关注阈值"}]}', 10001, NOW(), 10001, NOW(), 0, 0),
(80003, 20001, 'HPA_ADRENAL', 'HPA肾上腺压力轴', 'ENDOCRINE', '1.0.0', 'ACTIVE', '{"indicators":["cortisol_am","cortisol_pm","dhea_s"],"rules":[{"indicatorCode":"cortisol_am","condition":"GT","threshold":25.0,"riskLevel":"ATTENTION","evidence":"晨起皮质醇偏高"}]}', 10001, NOW(), 10001, NOW(), 0, 0),
(80004, 20001, 'THYROID_HORMONE', '甲状腺与性激素内分泌', 'ENDOCRINE', '1.0.0', 'ACTIVE', '{"indicators":["tsh","ft3","ft4","estradiol","testosterone"],"rules":[{"indicatorCode":"tsh","condition":"GT","threshold":4.2,"riskLevel":"ATTENTION","evidence":"TSH高于参考上限"}]}', 10001, NOW(), 10001, NOW(), 0, 0),
(80005, 20001, 'DETOX_GUT', '重金属/肝脏解毒/肠道屏障', 'DETOX', '1.0.0', 'ACTIVE', '{"indicators":["alt","ast","ggt","zonulin","heavy_metal_panel"],"rules":[{"indicatorCode":"alt","condition":"GT","threshold":40.0,"riskLevel":"ATTENTION","evidence":"ALT高于参考上限"}]}', 10001, NOW(), 10001, NOW(), 0, 0);
