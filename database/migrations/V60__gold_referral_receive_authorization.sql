CREATE TABLE IF NOT EXISTS gold_referral_authorization (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    app_id VARCHAR(64) NOT NULL,
    openid VARCHAR(128) NOT NULL,
    transfer_scene_id VARCHAR(36) NOT NULL,
    out_authorization_no VARCHAR(32) NOT NULL,
    authorization_id VARCHAR(64) NULL,
    state VARCHAR(32) NOT NULL,
    package_info VARCHAR(1024) NULL,
    authorization_created_at DATETIME NULL,
    authorized_at DATETIME NULL,
    last_checked_at DATETIME NULL,
    next_retry_at DATETIME NULL,
    failure_reason VARCHAR(255) NULL,
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_gold_referral_authorization_out_no (out_authorization_no),
    KEY idx_gold_referral_authorization_user (
        tenant_id, user_id, app_id, transfer_scene_id, state, created_at
    )
);
