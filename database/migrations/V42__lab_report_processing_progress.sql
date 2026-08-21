ALTER TABLE lab_report
    ADD COLUMN processing_progress INT NOT NULL DEFAULT 0 COMMENT '处理进度百分比' AFTER status,
    ADD COLUMN processing_message VARCHAR(120) NULL COMMENT '处理阶段提示' AFTER processing_progress;
