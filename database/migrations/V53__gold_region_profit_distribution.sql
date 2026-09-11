-- 区域盈利按推荐链自动结算：开辟人获得 100%，上级奖励另行追加。
-- A→B 时直接上级额外 20%；A→B→C 时直接上级额外 5%、祖级上级额外 15%。
-- 金额单位为整枚金豆；比例奖励不足 1 枚时向下取整，不产生小数金豆。

ALTER TABLE gold_region_profit
    ADD COLUMN owner_user_id BIGINT NULL AFTER region_id,
    ADD COLUMN owner_amount BIGINT NOT NULL DEFAULT 0 AFTER amount,
    ADD COLUMN retained_amount BIGINT NOT NULL DEFAULT 0 AFTER owner_amount,
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'SETTLED' AFTER retained_amount;

CREATE TABLE gold_region_profit_distribution (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    profit_id BIGINT NOT NULL,
    region_id BIGINT NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    recipient_type VARCHAR(20) NOT NULL,
    rate_percent INT NOT NULL,
    amount BIGINT NOT NULL,
    idempotency_key VARCHAR(180) NOT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_gold_region_profit_distribution_recipient (profit_id, recipient_user_id),
    UNIQUE KEY uk_gold_region_profit_distribution_idempotency (idempotency_key),
    KEY idx_gold_region_profit_distribution_region (tenant_id, region_id, created_at),
    KEY idx_gold_region_profit_distribution_recipient (tenant_id, recipient_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
