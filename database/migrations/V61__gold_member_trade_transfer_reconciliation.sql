-- 金豆集市转账改用商家转账接口，并保存等待确认收款所需的状态与参数。
-- transfer_detail_no 继续保存平台生成的 out_bill_no，保留旧版 D 后缀订单的兼容查询。
ALTER TABLE gold_member_trade
    ADD COLUMN transfer_wechat_state VARCHAR(32) NULL AFTER transfer_detail_no,
    ADD COLUMN transfer_package_info VARCHAR(1024) NULL AFTER transfer_wechat_state,
    ADD COLUMN transfer_last_checked_at DATETIME NULL AFTER transfer_package_info,
    ADD COLUMN transfer_next_retry_at DATETIME NULL AFTER transfer_last_checked_at,
    ADD KEY idx_gold_member_trade_transfer_recovery (status, transfer_next_retry_at, created_at);
