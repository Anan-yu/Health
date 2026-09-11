-- 集市买家入账规则：普通会员双账本各半，传奇人物只接收数字银行金豆。
ALTER TABLE gold_member_trade
    ADD COLUMN buyer_credit_mode VARCHAR(20) NOT NULL DEFAULT 'SPLIT' AFTER bucket,
    ADD KEY idx_gold_member_trade_buyer_credit_mode (tenant_id, buyer_credit_mode, status, created_at);
