-- Formal mini-program identity: store only an irreversible phone hash for matching.
ALTER TABLE sys_user
  ADD COLUMN phone_hash CHAR(64) NULL COMMENT '微信验证手机号的SHA-256匹配值' AFTER phone_masked,
  ADD UNIQUE KEY uk_sys_user_phone_hash (phone_hash);

-- Give existing development accounts a deterministic phone identity so local phone-login tests
-- continue to map to the expected workbench. No raw phone number is stored.
UPDATE sys_user SET phone_hash = SHA2('13800000002', 256) WHERE id = 10002 AND deleted = 0;
UPDATE sys_user SET phone_hash = SHA2('13800000003', 256) WHERE id = 10003 AND deleted = 0;
UPDATE sys_user SET phone_hash = SHA2('13800000004', 256) WHERE id = 10004 AND deleted = 0;
UPDATE sys_user SET phone_hash = SHA2('13800000005', 256) WHERE id = 10005 AND deleted = 0;
