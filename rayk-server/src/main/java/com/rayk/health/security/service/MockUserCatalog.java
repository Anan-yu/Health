package com.rayk.health.security.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 内存固定开发账号目录。
 *
 * @deprecated 已被 {@link DatabaseUserCatalog} 取代。默认 rayk.auth.mode=database 时不会装配本类，
 *     仅当显式设置 rayk.auth.mode=mock 时才启用，用于无数据库的极简本地调试。
 */
@Deprecated
@Component
@ConditionalOnProperty(name = "rayk.auth.mode", havingValue = "mock", matchIfMissing = false)
public class MockUserCatalog implements UserCatalog {
    private static final String HASH = "$2b$12$a3HfX53cvu1FWiTpSPjlh.o3SE16baDRxiMxEHjejlP4fLbG3OYXC";
    private final Map<String, MockAccount> accounts = new LinkedHashMap<>();

    public MockUserCatalog() {
        add(
                10001,
                1,
                "RayK平台",
                "platform_admin",
                "平台管理员",
                List.of("PLATFORM_ADMIN"),
                List.of("platform:tenant:list", "platform:audit:list"),
                List.of(new WorkbenchOption("PLATFORM_ADMIN", "平台管理工作台")));
        add(
                10002,
                20001,
                "RayK测试健康中心",
                "tenant_admin",
                "机构管理员",
                List.of("TENANT_ADMIN"),
                List.of(
                        "patient:list",
                        "patient:create",
                        "lab-report:manage",
                        "assessment:list",
                        "followup:manage",
                        "tenant:staff:manage"),
                List.of(new WorkbenchOption("TENANT_ADMIN", "机构管理工作台")));
        add(
                10003,
                20001,
                "RayK测试健康中心",
                "doctor",
                "测试医生",
                List.of("DOCTOR", "CUSTOMER"),
                List.of(
                        "patient:list",
                        "patient:detail",
                        "assessment:review",
                        "report:publish",
                        "self:health-record"),
                List.of(
                        new WorkbenchOption("DOCTOR", "医生工作台"),
                        new WorkbenchOption("CUSTOMER", "个人健康中心")));
        add(
                10004,
                20001,
                "RayK测试健康中心",
                "health_manager",
                "测试健康管理师",
                List.of("HEALTH_MANAGER"),
                List.of(
                        "patient:list",
                        "patient:create",
                        "followup:manage"),
                List.of(new WorkbenchOption("HEALTH_MANAGER", "健康管理工作台")));
        add(
                10005,
                20001,
                "RayK测试健康中心",
                "customer",
                "测试客户",
                List.of("CUSTOMER"),
                List.of(
                        "self:health-record",
                        "self:lab-report",
                        "self:assessment",
                        "self:health-report",
                        "self:followup"),
                List.of(new WorkbenchOption("CUSTOMER", "个人健康中心")));
    }

    private void add(
            long userId,
            long tenantId,
            String tenantName,
            String username,
            String displayName,
            List<String> roles,
            List<String> permissions,
            List<WorkbenchOption> workbenches) {
        accounts.put(
                username,
                new MockAccount(
                        userId,
                        tenantId,
                        tenantName,
                        username,
                        displayName,
                        HASH,
                        roles,
                        permissions,
                        workbenches,
                        workbenches.getFirst().code()));
    }

    @Override
    public UserAccount findByUsername(String username) {
        MockAccount account = accounts.get(username);
        return account == null ? null : toUserAccount(account);
    }

    @Override
    public UserAccount findByUserId(long userId) {
        return accounts.values().stream()
                .filter(account -> account.userId() == userId)
                .findFirst()
                .map(MockUserCatalog::toUserAccount)
                .orElse(null);
    }

    @Override
    public UserAccount findByPhoneHash(String phoneHash) {
        return null;
    }

    @Override
    public List<UserAccount> all() {
        return accounts.values().stream().map(MockUserCatalog::toUserAccount).toList();
    }

    private static UserAccount toUserAccount(MockAccount account) {
        return new UserAccount(
                account.userId(),
                account.tenantId(),
                account.tenantName(),
                account.username(),
                account.displayName(),
                account.passwordHash(),
                "ACTIVE",
                account.roles(),
                account.permissions(),
                account.workbenches(),
                account.defaultWorkbench());
    }
}
