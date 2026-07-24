-- Controlled health follow-up cycle: structured feedback, bounded continuation and reminders.
ALTER TABLE followup_task
  ADD COLUMN parent_task_id BIGINT NULL COMMENT 'previous task in the same follow-up cycle' AFTER plan_id,
  ADD COLUMN cycle_no INT NOT NULL DEFAULT 1 COMMENT 'current cycle number' AFTER parent_task_id,
  ADD COLUMN max_cycles INT NOT NULL DEFAULT 4 COMMENT 'hard cycle limit' AFTER cycle_no,
  ADD COLUMN completion_rate INT NULL COMMENT 'structured action completion percentage' AFTER feedback,
  ADD COLUMN feedback_detail JSON NULL COMMENT 'structured action feedback' AFTER completion_rate,
  ADD COLUMN decision VARCHAR(20) NULL COMMENT 'CONTINUE ADJUST TERMINATE PAUSE' AFTER feedback_detail,
  ADD COLUMN decision_reason VARCHAR(500) NULL COMMENT 'follow-up decision explanation' AFTER decision,
  ADD COLUMN reminder_count INT NOT NULL DEFAULT 0 COMMENT 'number of in-app reminders generated' AFTER decision_reason,
  ADD COLUMN last_reminded_at DATETIME NULL COMMENT 'last reminder generation time' AFTER reminder_count,
  ADD KEY idx_followup_task_parent (tenant_id, parent_task_id),
  ADD KEY idx_followup_task_reminder (status, due_date, last_reminded_at);

UPDATE followup_task
SET cycle_no = 1,
    max_cycles = 4,
    reminder_count = 0
WHERE deleted = 0;
