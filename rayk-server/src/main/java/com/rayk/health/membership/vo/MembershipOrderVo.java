package com.rayk.health.membership.vo;

import java.time.LocalDateTime;

public record MembershipOrderVo(
        String orderNo,
        String planCode,
        String planName,
        String status,
        int amountCent,
        boolean simulated,
        boolean paymentEnabled,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime expireAt) {}
