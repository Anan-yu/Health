ALTER TABLE lab_report
    ADD COLUMN image_analysis_snapshot JSON NULL COMMENT 'qwen图片直读逐页结构化事实快照' AFTER ocr_snapshot;
