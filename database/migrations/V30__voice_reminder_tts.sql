CREATE TABLE voice_reminder_setting (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    meal_enabled TINYINT NOT NULL DEFAULT 1,
    meal_time TIME NOT NULL DEFAULT '11:30:00',
    sleep_enabled TINYINT NOT NULL DEFAULT 1,
    sleep_time TIME NOT NULL DEFAULT '21:30:00',
    timezone VARCHAR(64) NOT NULL DEFAULT 'Asia/Shanghai',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_voice_reminder_setting_user (tenant_id, user_id),
    KEY idx_voice_reminder_setting_due (meal_enabled, meal_time, sleep_enabled, sleep_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE voice_reminder_audio (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reminder_type VARCHAR(16) NOT NULL,
    text_content VARCHAR(300) NOT NULL,
    voice_type INT NOT NULL,
    voice_name VARCHAR(64) NOT NULL,
    object_path VARCHAR(500) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_by BIGINT NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_voice_reminder_audio_user (tenant_id, user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
