-- AI provider model selection managed by the platform administrator.
-- API keys and provider credentials remain in environment secrets; this table
-- stores only the non-sensitive model metadata and the selected model.
CREATE TABLE ai_model_runtime_config (
  id BIGINT NOT NULL,
  model_code VARCHAR(80) NOT NULL,
  model_name VARCHAR(120) NOT NULL,
  provider VARCHAR(40) NOT NULL,
  model_version VARCHAR(80) NOT NULL,
  base_url VARCHAR(255) NOT NULL,
  context_length_tokens INT NOT NULL,
  max_output_tokens INT NOT NULL,
  thinking_supported TINYINT NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  selected TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_by BIGINT NOT NULL,
  updated_at DATETIME NOT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  optimistic_version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ai_model_runtime_code (model_code),
  KEY idx_ai_model_runtime_selected (status, selected)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型运行时配置';

INSERT INTO ai_model_runtime_config
  (id, model_code, model_name, provider, model_version, base_url,
   context_length_tokens, max_output_tokens, thinking_supported, status,
   selected, created_by, created_at, updated_by, updated_at, deleted,
   optimistic_version)
VALUES
  (84001, 'deepseek-v4-flash', 'DeepSeek V4 Flash', 'DeepSeek',
   'DeepSeek-V4-Flash-0731', 'https://api.deepseek.com',
   1048576, 393216, 1, 'ACTIVE', 1, 10001, NOW(), 10001, NOW(), 0, 0),
  (84002, 'deepseek-v4-pro', 'DeepSeek V4 Pro', 'DeepSeek',
   'DeepSeek-V4-Pro-0813', 'https://api.deepseek.com',
   1048576, 393216, 1, 'ACTIVE', 0, 10001, NOW(), 10001, NOW(), 0, 0);
