package com.rayk.health.platform.vo;

import java.util.List;

public record PlatformOverviewVo(
        long tenantCount,
        long activeTenantCount,
        long userCount,
        long patientCount,
        long pendingReviewCount,
        long pendingFollowupCount,
        long todayFollowupCount,
        List<TenantSummaryVo> tenants,
        List<PlatformFollowupVo> followups) {}
