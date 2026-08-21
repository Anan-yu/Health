package com.rayk.health.membership.vo;

public record MembershipBenefitVo(
        String benefitCode,
        String benefitName,
        String description,
        String unitType,
        Integer quota,
        int used,
        Integer remaining,
        boolean available) {}
