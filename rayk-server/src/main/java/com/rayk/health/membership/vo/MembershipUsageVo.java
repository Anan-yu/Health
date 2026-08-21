package com.rayk.health.membership.vo;

import java.time.LocalDateTime;

public record MembershipUsageVo(
        String benefitCode,
        String benefitName,
        String usageStatus,
        String bizType,
        String bizId,
        LocalDateTime occurredAt) {}
