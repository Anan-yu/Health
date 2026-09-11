-- 健康树洞复用健康助手的消息存储，但按会话类型隔离，避免互相污染历史上下文。
ALTER TABLE medical_assistant_conversation
    ADD COLUMN conversation_type VARCHAR(32) NOT NULL DEFAULT 'MEDICAL_ASSISTANT' AFTER model,
    ADD KEY idx_medical_assistant_conversation_type_customer
        (tenant_id, patient_id, conversation_type, updated_at);

-- 每个客户每个七天周期最多生成一份反馈，反馈只保留摘要和安全字段，不重复保存原始健康报告。
CREATE TABLE health_tree_hole_feedback (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    recorded_days INT NOT NULL DEFAULT 0,
    entry_count INT NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    risk_level VARCHAR(16) NULL,
    recommended_action VARCHAR(800) NULL,
    model VARCHAR(80) NULL,
    generated_at DATETIME NOT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_health_tree_hole_feedback_period
        (tenant_id, patient_id, period_start, period_end),
    KEY idx_health_tree_hole_feedback_customer
        (tenant_id, patient_id, period_end, generated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
