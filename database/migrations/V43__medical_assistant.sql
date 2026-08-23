CREATE TABLE medical_assistant_conversation (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    model VARCHAR(80) NOT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_medical_assistant_conversation_customer (tenant_id, patient_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE medical_assistant_message (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    risk_level VARCHAR(16) NULL,
    emergency TINYINT NOT NULL DEFAULT 0,
    recommended_action VARCHAR(800) NULL,
    citations_json TEXT NULL,
    used_context_json TEXT NULL,
    followup_questions_json TEXT NULL,
    model VARCHAR(80) NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_medical_assistant_message_conversation (conversation_id, created_at),
    KEY idx_medical_assistant_message_customer (tenant_id, patient_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO membership_benefit
    (id, benefit_code, benefit_name, description, unit_type, created_at, updated_at)
SELECT
    320000001011,
    'AI_MEDICAL_ASSISTANT',
    '三羊健康助手',
    '结合本人健康档案、健康评估和健康拍进行健康管理问答。',
    'YEARLY',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM membership_benefit
    WHERE benefit_code = 'AI_MEDICAL_ASSISTANT' AND deleted = 0
);

INSERT INTO membership_plan_benefit
    (id, plan_id, benefit_code, quota_value, created_at, updated_at)
SELECT
    320000002016,
    plan.id,
    'AI_MEDICAL_ASSISTANT',
    3,
    NOW(),
    NOW()
FROM membership_plan plan
WHERE plan.plan_code = 'FREE_CUSTOMER'
  AND plan.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM membership_plan_benefit existing
      WHERE existing.plan_id = plan.id
        AND existing.benefit_code = 'AI_MEDICAL_ASSISTANT'
        AND existing.deleted = 0
  );

INSERT INTO membership_plan_benefit
    (id, plan_id, benefit_code, quota_value, created_at, updated_at)
SELECT
    320000002017,
    plan.id,
    'AI_MEDICAL_ASSISTANT',
    NULL,
    NOW(),
    NOW()
FROM membership_plan plan
WHERE plan.plan_code = 'AI_HEALTH_YEARLY'
  AND plan.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM membership_plan_benefit existing
      WHERE existing.plan_id = plan.id
        AND existing.benefit_code = 'AI_MEDICAL_ASSISTANT'
        AND existing.deleted = 0
  );
