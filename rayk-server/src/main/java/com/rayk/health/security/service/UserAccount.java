package com.rayk.health.security.service;

import java.util.List;

/**
 * 统一账号视图：屏蔽底层账号来源（内存 Mock 或数据库 RBAC）的差异。
 *
 * <p>由 {@link UserCatalog} 装配，供认证、资料、工作台等场景消费。
 */
public record UserAccount(
        long userId,
        long tenantId,
        String tenantName,
        String username,
        String displayName,
        String passwordHash,
        String status,
        List<String> roles,
        List<String> permissions,
        List<WorkbenchOption> workbenches,
        String defaultWorkbench) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
