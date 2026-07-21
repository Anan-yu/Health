package com.rayk.health.platform.vo;

public record TenantSummaryVo(
        String id, String code, String name, String status, String servicePlan, long userCount) {}
