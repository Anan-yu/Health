package com.rayk.health.security.service;

import java.util.List;

public record MockAccount(
        long userId,
        long tenantId,
        String tenantName,
        String username,
        String displayName,
        String passwordHash,
        List<String> roles,
        List<String> permissions,
        List<WorkbenchOption> workbenches,
        String defaultWorkbench) {}

