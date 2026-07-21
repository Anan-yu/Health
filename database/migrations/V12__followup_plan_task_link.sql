-- Link generated follow-up tasks to their owning plan so adjacent plans cannot
-- accidentally include each other's tasks when their date ranges overlap.
ALTER TABLE followup_task
  ADD COLUMN plan_id BIGINT NULL AFTER patient_id,
  ADD KEY idx_followup_task_plan (tenant_id, plan_id);
