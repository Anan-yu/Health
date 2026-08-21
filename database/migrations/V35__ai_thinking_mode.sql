-- Persist the platform administrator's thinking-mode choice.
ALTER TABLE ai_model_runtime_config
  ADD COLUMN thinking_enabled TINYINT NOT NULL DEFAULT 0 AFTER thinking_supported;

