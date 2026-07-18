package com.rayk.health.laboratory.vo;

import java.math.BigDecimal;

public record IndicatorVo(
        String id,
        String code,
        String name,
        BigDecimal value,
        String unit,
        BigDecimal referenceLow,
        BigDecimal referenceHigh,
        String abnormalFlag,
        boolean manuallyConfirmed) {}

