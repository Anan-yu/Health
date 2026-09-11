-- 新会员注册费统一为 1000 元；用户虚拟支付金额由应用按 12% 加价计算为 1120 元。
-- 已支付账户、已创建订单和历史推荐结算保留原始金额，不追溯改价。

ALTER TABLE gold_member_account
    MODIFY COLUMN registration_fee_cent INT NOT NULL DEFAULT 100000;

ALTER TABLE gold_member_referral
    MODIFY COLUMN registration_fee_cent INT NOT NULL DEFAULT 100000;

UPDATE gold_member_account
SET registration_fee_cent = 100000
WHERE registration_fee_status <> 'PAID';
