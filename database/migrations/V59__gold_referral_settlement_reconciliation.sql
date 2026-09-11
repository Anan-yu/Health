ALTER TABLE gold_member_order
    ADD COLUMN settlement_wechat_state VARCHAR(32) NULL AFTER settlement_detail_no,
    ADD COLUMN settlement_package_info VARCHAR(1024) NULL AFTER settlement_wechat_state,
    ADD COLUMN settlement_last_checked_at DATETIME NULL AFTER settlement_package_info,
    ADD COLUMN settlement_next_retry_at DATETIME NULL AFTER settlement_last_checked_at;
