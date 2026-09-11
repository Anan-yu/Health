-- 个人推荐码只属于已完成会员注册的账号。
-- MySQL 唯一索引允许多个 NULL，因此未注册账号可以安全地保持无推荐码状态。
ALTER TABLE gold_member_account
    MODIFY COLUMN referral_code VARCHAR(32) NULL DEFAULT NULL;

UPDATE gold_member_account
SET referral_code = NULL
WHERE registration_fee_status <> 'PAID'
  AND referral_code IS NOT NULL;
