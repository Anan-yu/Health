-- 传奇人物手机号白名单，以及可交易金/数字银行金分桶挂单。
-- 传奇资格是业务能力，不是新的登录角色；手机号只保存哈希与掩码。
ALTER TABLE gold_member_trade_listing
    ADD COLUMN bucket VARCHAR(20) NOT NULL DEFAULT 'TRADING' AFTER seller_user_id,
    ADD KEY idx_gold_trade_listing_bucket (tenant_id, bucket, status, created_at);

ALTER TABLE gold_member_trade
    ADD COLUMN bucket VARCHAR(20) NOT NULL DEFAULT 'TRADING' AFTER buyer_user_id,
    ADD KEY idx_gold_member_trade_bucket (tenant_id, bucket, created_at);

CREATE TABLE gold_member_legendary (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    phone_hash CHAR(64) NOT NULL,
    phone_masked VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    note VARCHAR(255) NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_gold_member_legendary_phone (phone_hash),
    KEY idx_gold_member_legendary_status (status, deleted, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
