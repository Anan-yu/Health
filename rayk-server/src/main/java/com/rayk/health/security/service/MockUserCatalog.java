package com.rayk.health.security.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Minimal offline-only catalog matching the formal three-role model. */
@Deprecated
@Component
@ConditionalOnProperty(name = "rayk.auth.mode", havingValue = "mock", matchIfMissing = false)
public class MockUserCatalog implements UserCatalog {
    private static final String HASH = "$2b$12$a3HfX53cvu1FWiTpSPjlh.o3SE16baDRxiMxEHjejlP4fLbG3OYXC";
    private final Map<String, UserAccount> accounts = new LinkedHashMap<>();

    public MockUserCatalog() {
        add(10001, 1, "致宇平台", "platform_admin", "平台管理员", List.of("PLATFORM_ADMIN"),
                List.of("platform:tenant:list"), "PLATFORM_ADMIN", "平台管理工作台");
        add(10003, 20001, "Rayk测试健康中心", "doctor", "测试医生", List.of("DOCTOR"),
                List.of("patient:list", "assessment:list"), "DOCTOR", "医生工作台");
        add(10005, 20001, "Rayk测试健康中心", "customer", "测试客户", List.of("CUSTOMER"),
                List.of("self:health-record", "self:lab-report", "self:assessment", "self:health-report", "self:followup"),
                "CUSTOMER", "个人健康中心");
    }

    private void add(long id, long tenantId, String tenantName, String username, String name, List<String> roles,
            List<String> permissions, String workbench, String workbenchName) {
        accounts.put(username, new UserAccount(id, tenantId, tenantName, username, name, HASH, "ACTIVE", roles,
                permissions, List.of(new WorkbenchOption(workbench, workbenchName)), workbench));
    }
    @Override public UserAccount findByUsername(String username) { return accounts.get(username); }
    @Override public UserAccount findByUserId(long userId) { return accounts.values().stream().filter(a -> a.userId() == userId).findFirst().orElse(null); }
    @Override public UserAccount findByPhoneHash(String phoneHash) { return null; }
    @Override public List<UserAccount> all() { return List.copyOf(accounts.values()); }
}
