package com.rayk.health.goldbean.vo;

import java.time.LocalDateTime;

public record GoldRegionVo(
        String id,
        String city,
        int depth,
        String parentRegionId,
        String status,
        LocalDateTime createdAt) {}
