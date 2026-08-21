ALTER TABLE membership_order
    ADD COLUMN transaction_id VARCHAR(64) NULL AFTER payment_channel,
    ADD COLUMN payment_notify_at DATETIME NULL AFTER paid_at,
    ADD UNIQUE KEY uk_membership_order_transaction_id (transaction_id);
