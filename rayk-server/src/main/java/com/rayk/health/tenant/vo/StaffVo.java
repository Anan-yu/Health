package com.rayk.health.tenant.vo;

import java.util.List;

public record StaffVo(
        String id,
        String username,
        String displayName,
        String phoneMasked,
        List<String> roles,
        String status) {}
