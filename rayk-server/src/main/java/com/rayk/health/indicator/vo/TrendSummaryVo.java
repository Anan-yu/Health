package com.rayk.health.indicator.vo;

import java.math.BigDecimal;

public record TrendSummaryVo(
        String indicatorCode,
        String indicatorName,
        BigDecimal latestValue,
        BigDecimal minValue,
        BigDecimal maxValue,
        BigDecimal averageValue,
        String unit,
        String trendDirection,
        int dataPoints) {}
