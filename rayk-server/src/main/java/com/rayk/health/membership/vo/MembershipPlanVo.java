package com.rayk.health.membership.vo;

import java.util.List;

public record MembershipPlanVo(
        String planCode,
        String planName,
        int durationDays,
        int priceCent,
        Integer originalPriceCent,
        String description,
        List<String> benefits) {}
