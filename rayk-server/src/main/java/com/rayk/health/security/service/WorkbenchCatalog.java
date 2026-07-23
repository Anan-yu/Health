package com.rayk.health.security.service;

import java.util.Map;

public final class WorkbenchCatalog {
    private static final Map<String, String> NAMES = Map.of(
            "PLATFORM_ADMIN", "平台管理工作台", "DOCTOR", "医生工作台", "CUSTOMER", "个人健康中心");
    private WorkbenchCatalog() {}
    public static String displayName(String code) { return NAMES.getOrDefault(code, code); }
}
