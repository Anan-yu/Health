package com.rayk.health.platform.vo;

import java.time.LocalDateTime;

/** The plaintext code is returned only once when the administrator creates it. */
public record PlatformGoldBeanInviteCreatedVo(
        String id,
        String code,
        String codeMasked,
        String status,
        String boundPhoneMasked,
        LocalDateTime expiresAt) {}
