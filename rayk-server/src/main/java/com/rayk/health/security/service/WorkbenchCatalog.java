package com.rayk.health.security.service;

import java.util.Map;

/**
 * 工作台编码到展示名称的映射。
 *
 * <p>sys_user_workbench 仅存储工作台编码，展示名称在此集中维护，
 * 避免在数据库中再引入一张工作台字典表。
 */
public final class WorkbenchCatalog {
    private static final Map<String, String> NAMES =
            Map.of(
                    "PLATFORM_ADMIN", "平台管理工作台",
                    "TENANT_ADMIN", "机构管理工作台",
                    "DOCTOR", "医生工作台",
                    "HEALTH_MANAGER", "健康管理工作台",
                    "CUSTOMER", "个人健康中心");

    private WorkbenchCatalog() {}

    public static String displayName(String code) {
        return NAMES.getOrDefault(code, code);
    }
}
