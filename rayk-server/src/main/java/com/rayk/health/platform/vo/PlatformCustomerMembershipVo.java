package com.rayk.health.platform.vo;

import java.time.LocalDateTime;

/** Minimal cross-tenant membership view for platform administrators. */
public record PlatformCustomerMembershipVo(
        String userId,
        String displayName,
        String phoneMasked,
        String membershipStatus,
        String planName,
        boolean active,
        LocalDateTime expireAt) {}
