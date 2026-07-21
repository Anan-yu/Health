package com.rayk.health.indicator.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrendPointVo(
        String reportId,
        LocalDate reportDate,
        BigDecimal value,
        String unit,
        String abnormalFlag) {}
