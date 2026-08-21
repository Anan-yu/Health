ALTER TABLE lab_report
    ADD COLUMN processing_progress INT NOT NULL DEFAULT 0;

ALTER TABLE lab_report
    ADD COLUMN processing_message VARCHAR(120) NULL;
