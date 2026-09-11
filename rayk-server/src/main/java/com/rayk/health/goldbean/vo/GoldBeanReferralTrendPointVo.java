package com.rayk.health.goldbean.vo;

import java.time.LocalDate;

public record GoldBeanReferralTrendPointVo(
        LocalDate date,
        int directReferralCount,
        String levelName) {}
