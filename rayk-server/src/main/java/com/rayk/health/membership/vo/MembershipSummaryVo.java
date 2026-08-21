package com.rayk.health.membership.vo;

import java.time.LocalDateTime;
import java.util.List;

public record MembershipSummaryVo(
        String membershipStatus,
        String planCode,
        String planName,
        LocalDateTime startAt,
        LocalDateTime expireAt,
        long remainingDays,
        boolean active,
        boolean paymentEnabled,
        List<MembershipBenefitVo> benefits) {}
