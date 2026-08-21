CREATE TABLE membership_plan (
    id BIGINT NOT NULL,
    plan_code VARCHAR(64) NOT NULL,
    plan_name VARCHAR(128) NOT NULL,
    duration_days INT NOT NULL DEFAULT 0,
    price_cent INT NOT NULL DEFAULT 0,
    original_price_cent INT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_plan_code (plan_code),
    KEY idx_membership_plan_status (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_membership (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    start_at DATETIME NOT NULL,
    expire_at DATETIME NOT NULL,
    source VARCHAR(32) NOT NULL DEFAULT 'SYSTEM',
    order_no VARCHAR(64) NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_customer_membership_customer (tenant_id, customer_id, status, expire_at),
    KEY idx_customer_membership_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE membership_benefit (
    id BIGINT NOT NULL,
    benefit_code VARCHAR(64) NOT NULL,
    benefit_name VARCHAR(128) NOT NULL,
    description VARCHAR(500) NULL,
    unit_type VARCHAR(24) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_benefit_code (benefit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE membership_plan_benefit (
    id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    benefit_code VARCHAR(64) NOT NULL,
    quota_value INT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_plan_benefit (plan_id, benefit_code),
    KEY idx_membership_plan_benefit_code (benefit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE membership_usage (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    membership_id BIGINT NULL,
    benefit_code VARCHAR(64) NOT NULL,
    biz_type VARCHAR(64) NOT NULL,
    biz_id VARCHAR(128) NULL,
    amount INT NOT NULL DEFAULT 1,
    usage_status VARCHAR(16) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    reserved_at DATETIME NOT NULL,
    confirmed_at DATETIME NULL,
    released_at DATETIME NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_usage_idempotency (idempotency_key),
    KEY idx_membership_usage_customer (tenant_id, customer_id, benefit_code, usage_status, created_at),
    KEY idx_membership_usage_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE membership_order (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    amount_cent INT NOT NULL,
    payment_channel VARCHAR(32) NULL,
    paid_at DATETIME NULL,
    expire_at DATETIME NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_order_no (order_no),
    KEY idx_membership_order_customer (tenant_id, customer_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO membership_plan
    (id, plan_code, plan_name, duration_days, price_cent, original_price_cent, status, description, sort_order, created_at, updated_at)
VALUES
    (320000000001, 'FREE_CUSTOMER', '免费客户', 0, 0, 0, 'ACTIVE', '保留健康档案、检验报告和历史数据，并提供一次性体验权益。', 10, NOW(), NOW()),
    (320000000002, 'AI_HEALTH_YEARLY', '年度健康会员', 365, 39900, 39900, 'ACTIVE', '解锁年度 AI 健康管理服务和会员专属权益。', 20, NOW(), NOW());

INSERT IGNORE INTO membership_benefit
    (id, benefit_code, benefit_name, description, unit_type, created_at, updated_at)
VALUES
    (320000001001, 'AI_HEALTH_REPORT', 'AI 健康评估', '生成综合健康评估和健康报告。', 'YEARLY', NOW(), NOW()),
    (320000001002, 'AI_FOLLOWUP_INITIAL', 'AI 初始随访', '生成首次健康随访计划。', 'LIFETIME', NOW(), NOW()),
    (320000001003, 'AI_FOLLOWUP_CONTINUE', 'AI 随访续期', '持续生成后续随访周期。', 'ENABLED', NOW(), NOW()),
    (320000001004, 'HEALTH_SHOT', '健康拍', '进行面部健康检测。', 'LIFETIME', NOW(), NOW()),
    (320000001005, 'HEALTH_SHOT_DAILY', '健康拍每日额度', '会员每日健康拍额度。', 'DAILY', NOW(), NOW()),
    (320000001006, 'HEALTH_SHOT_30D', '健康拍滚动额度', '会员滚动 30 天健康拍额度。', 'ROLLING_30D', NOW(), NOW()),
    (320000001007, 'TTS_MEAL_REMINDER', '吃饭语音提醒', '生成吃饭语音提醒。', 'LIFETIME', NOW(), NOW()),
    (320000001008, 'TTS_SLEEP_REMINDER', '睡眠语音提醒', '生成睡眠语音提醒。', 'LIFETIME', NOW(), NOW()),
    (320000001009, 'AI_REPORT_REGENERATE', '报告重新解读', '会员按报告重新生成 AI 解读。', 'PER_REPORT', NOW(), NOW());

INSERT IGNORE INTO membership_plan_benefit
    (id, plan_id, benefit_code, quota_value, created_at, updated_at)
VALUES
    (320000002001, 320000000001, 'AI_HEALTH_REPORT', 1, NOW(), NOW()),
    (320000002002, 320000000001, 'AI_FOLLOWUP_INITIAL', 1, NOW(), NOW()),
    (320000002003, 320000000001, 'HEALTH_SHOT', 1, NOW(), NOW()),
    (320000002004, 320000000001, 'TTS_MEAL_REMINDER', 1, NOW(), NOW()),
    (320000002005, 320000000001, 'TTS_SLEEP_REMINDER', 1, NOW(), NOW()),
    (320000002006, 320000000002, 'AI_HEALTH_REPORT', 12, NOW(), NOW()),
    (320000002007, 320000000002, 'AI_FOLLOWUP_CONTINUE', NULL, NOW(), NOW()),
    (320000002008, 320000000002, 'HEALTH_SHOT_DAILY', 1, NOW(), NOW()),
    (320000002009, 320000000002, 'HEALTH_SHOT_30D', 30, NOW(), NOW()),
    (320000002010, 320000000002, 'TTS_MEAL_REMINDER', NULL, NOW(), NOW()),
    (320000002011, 320000000002, 'TTS_SLEEP_REMINDER', NULL, NOW(), NOW()),
    (320000002012, 320000000002, 'AI_REPORT_REGENERATE', 3, NOW(), NOW());
