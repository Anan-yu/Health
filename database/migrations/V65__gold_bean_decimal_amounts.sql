-- 金豆统一使用 DECIMAL(24,6) 定点小数。历史整型数据会自动转换为带六位小数的值。
-- 人民币金额仍使用“分”保存，不在本迁移中改变支付金额字段。

ALTER TABLE gold_member_account
    MODIFY COLUMN digital_bank_balance DECIMAL(24,6) NOT NULL DEFAULT 30.000000,
    MODIFY COLUMN trading_balance DECIMAL(24,6) NOT NULL DEFAULT 30.000000;

ALTER TABLE gold_member_ledger
    MODIFY COLUMN amount DECIMAL(24,6) NOT NULL;

ALTER TABLE gold_member_order
    MODIFY COLUMN gold_bean_quantity DECIMAL(24,6) NOT NULL DEFAULT 0.000000;

ALTER TABLE gold_member_trade_listing
    MODIFY COLUMN quantity DECIMAL(24,6) NOT NULL,
    MODIFY COLUMN remaining_quantity DECIMAL(24,6) NOT NULL;

ALTER TABLE gold_member_trade
    MODIFY COLUMN quantity DECIMAL(24,6) NOT NULL;

ALTER TABLE gold_region_profit
    MODIFY COLUMN amount DECIMAL(24,6) NOT NULL,
    MODIFY COLUMN owner_amount DECIMAL(24,6) NOT NULL DEFAULT 0.000000,
    MODIFY COLUMN retained_amount DECIMAL(24,6) NOT NULL DEFAULT 0.000000;

ALTER TABLE gold_region_profit_distribution
    MODIFY COLUMN amount DECIMAL(24,6) NOT NULL;

ALTER TABLE gold_robot_redemption
    MODIFY COLUMN gold_bean_cost DECIMAL(24,6) NOT NULL DEFAULT 10000.000000;
