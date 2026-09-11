-- 每位用户的机器人权益只能兑换一次。
-- V55 已保存历史兑换记录；唯一约束也覆盖软删除记录，避免通过重复兑换绕过一次性规则。

ALTER TABLE gold_robot_redemption
    ADD UNIQUE KEY uk_gold_robot_redemption_user_entitlement (tenant_id, user_id, entitlement_code);
