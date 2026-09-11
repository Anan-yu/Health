-- Keep the buyer's virtual-payment amount separate from the business amount paid to a
-- referrer/seller. Existing records are backfilled with their historical amount so callbacks
-- and reconciliation remain compatible with orders created before the surcharge was enabled.

ALTER TABLE gold_member_order
    ADD COLUMN payment_amount_cent INT NULL
        COMMENT '用户虚拟支付应付金额（分），amount_cent 保留业务/结算金额'
        AFTER amount_cent;

UPDATE gold_member_order
SET payment_amount_cent = amount_cent
WHERE payment_amount_cent IS NULL;

ALTER TABLE gold_member_trade
    ADD COLUMN payment_unit_price_cent BIGINT NULL
        COMMENT '买家虚拟支付单价（分），unit_price_cent 保留卖家结算单价'
        AFTER unit_price_cent;

ALTER TABLE gold_member_trade
    ADD COLUMN payment_amount BIGINT NULL
        COMMENT '买家虚拟支付总额（分），total_amount 保留卖家结算总额'
        AFTER total_amount;

UPDATE gold_member_trade
SET payment_unit_price_cent = unit_price_cent,
    payment_amount = total_amount
WHERE payment_unit_price_cent IS NULL
   OR payment_amount IS NULL;
