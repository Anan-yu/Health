-- V7: 为异步任务恢复调度器添加复合索引，加速按状态+时间扫描卡住的任务
CREATE INDEX idx_ai_task_recovery ON ai_task(status, started_at, created_at);
