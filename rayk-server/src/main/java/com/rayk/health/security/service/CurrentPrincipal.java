package com.rayk.health.security.service;

import java.util.List;

public record CurrentPrincipal(
        String jti,
        String username,
        long userId,
        long tenantId,
        List<String> roles,
        List<String> permissions,
        String workbench) {}

