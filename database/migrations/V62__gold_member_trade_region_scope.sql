-- 金豆集市按注册区域城市划分普通用户可交易范围。
-- 挂单保存卖家注册城市快照，避免仅靠前端筛选或卖家资料变化绕过区域规则。
ALTER TABLE gold_member_trade_listing
    ADD COLUMN region_city VARCHAR(64) NULL AFTER bucket,
    ADD KEY idx_gold_trade_listing_region_market (tenant_id, region_city, bucket, status, created_at);

-- 为 V62 之前创建的挂单补齐卖家注册城市；无法补齐的历史数据只对传奇用户保留可见，
-- 普通用户的市场查询和买入校验都会拒绝无区域挂单。
UPDATE gold_member_trade_listing listing
JOIN gold_member_account account
    ON account.tenant_id = listing.tenant_id
   AND account.user_id = listing.seller_user_id
   AND account.deleted = 0
SET listing.region_city = NULLIF(TRIM(account.city), '')
WHERE listing.region_city IS NULL
  AND account.registration_fee_status = 'PAID'
  AND account.status = 'ACTIVE';
