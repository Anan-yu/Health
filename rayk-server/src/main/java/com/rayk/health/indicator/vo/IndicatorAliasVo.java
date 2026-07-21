package com.rayk.health.indicator.vo;

import java.time.LocalDateTime;

public record IndicatorAliasVo(
        String id,
        String indicatorCode,
        String aliasName,
        String source,
        LocalDateTime createdAt) {}
