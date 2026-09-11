ALTER TABLE gold_member_order
    ADD COLUMN registration_referrer_id BIGINT NULL AFTER registration_referral_code,
    ADD COLUMN settlement_status VARCHAR(24) NOT NULL DEFAULT 'NOT_REQUIRED' AFTER registration_fee_recipient,
    ADD COLUMN settlement_batch_no VARCHAR(80) NULL AFTER settlement_status,
    ADD COLUMN settlement_detail_no VARCHAR(80) NULL AFTER settlement_batch_no,
    ADD COLUMN settlement_failure_reason VARCHAR(255) NULL AFTER settlement_detail_no,
    ADD COLUMN settled_at DATETIME NULL AFTER settlement_failure_reason,
    ADD KEY idx_gold_member_order_referrer (tenant_id, registration_referrer_id, created_at),
    ADD KEY idx_gold_member_order_settlement (tenant_id, settlement_status, created_at);
