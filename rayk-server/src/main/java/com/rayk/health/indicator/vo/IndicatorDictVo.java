package com.rayk.health.indicator.vo;

import java.time.LocalDateTime;

public record IndicatorDictVo(
        String id,
        String indicatorCode,
        String indicatorName,
        String standardUnit,
        String categoryCode,
        boolean enabled,
        LocalDateTime createdAt) {}
