package com.rayk.health.platform.vo;

import java.time.LocalDateTime;

public record PlatformGoldRegionVo(
        String id,
        String tenantId,
        String tenantName,
        String city,
        int depth,
        String parentRegionId,
        String ownerUserId,
        String ownerName,
        String ownerPhoneMasked,
        String memberLevelName,
        String status,
        LocalDateTime createdAt) {}
