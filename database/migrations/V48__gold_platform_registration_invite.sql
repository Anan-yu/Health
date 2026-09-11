CREATE TABLE IF NOT EXISTS gold_platform_registration_invite (
    id BIGINT NOT NULL,
    invite_code_hash CHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    bound_phone_masked VARCHAR(32) NULL,
    bound_phone_hash CHAR(64) NULL,
    reserved_order_no VARCHAR(80) NULL,
    consumed_order_no VARCHAR(80) NULL,
    expires_at DATETIME NOT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    consumed_at DATETIME NULL,
    revoked_at DATETIME NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_gold_platform_invite_hash (invite_code_hash),
    UNIQUE KEY uk_gold_platform_invite_reserved_order (reserved_order_no),
    UNIQUE KEY uk_gold_platform_invite_consumed_order (consumed_order_no),
    KEY idx_gold_platform_invite_status (status, expires_at),
    KEY idx_gold_platform_invite_bound_phone (bound_phone_hash, status)
);

ALTER TABLE gold_member_order
    ADD COLUMN platform_invite_id BIGINT NULL AFTER registration_fee_recipient,
    ADD COLUMN platform_slot_key VARCHAR(32) NULL AFTER platform_invite_id,
    ADD KEY idx_gold_member_order_platform_invite (platform_invite_id),
    ADD UNIQUE KEY uk_gold_member_order_platform_slot (platform_slot_key);
