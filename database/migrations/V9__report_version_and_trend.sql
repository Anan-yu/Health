-- V9: Add index for indicator trend queries and ensure report version support

-- Add composite index for trend queries on indicator_value
-- This index supports filtering by tenant, patient, indicator code, confirmation status, and time ordering
CREATE INDEX idx_indicator_trend ON indicator_value(tenant_id, patient_id, indicator_code, manually_confirmed, created_at);

-- Add object_path column to health_report (for direct report file reference)
-- The health_report_version table already has object_path, this adds a convenience column on health_report
ALTER TABLE health_report ADD COLUMN object_path VARCHAR(500) NULL COMMENT '报告文件对象路径' AFTER disclaimer;
