ALTER TABLE ai_task
  ADD COLUMN engine_version VARCHAR(80) NULL COMMENT 'AI/OCR引擎版本' AFTER status,
  ADD COLUMN confidence DECIMAL(5,4) NULL COMMENT '整体识别置信度' AFTER engine_version,
  ADD COLUMN result_snapshot JSON NULL COMMENT '任务结果快照' AFTER confidence,
  ADD COLUMN attempt_count INT NOT NULL DEFAULT 1 COMMENT '执行次数' AFTER result_snapshot,
  ADD KEY idx_ai_task_report_type (tenant_id, report_id, task_type, created_at);

