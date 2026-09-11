-- 数字银行金豆兑换机器人权益。
-- 每笔兑换固定消耗 10000 枚数字银行金豆；群二维码由部署环境配置，兑换时保存快照。

CREATE TABLE gold_robot_redemption (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    redemption_no VARCHAR(40) NOT NULL,
    entitlement_code VARCHAR(40) NOT NULL DEFAULT 'ROBOT',
    gold_bean_cost BIGINT NOT NULL DEFAULT 10000,
    group_name VARCHAR(128) NOT NULL,
    group_qr_image_url VARCHAR(1024) NOT NULL,
    client_request_id VARCHAR(80) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    redeemed_at DATETIME NOT NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_gold_robot_redemption_no (redemption_no),
    UNIQUE KEY uk_gold_robot_redemption_request (tenant_id, user_id, client_request_id),
    KEY idx_gold_robot_redemption_user (tenant_id, user_id, status, redeemed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
