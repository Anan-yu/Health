package com.rayk.health.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.tenant.TenantContext;
import java.util.Set;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class MybatisPlusConfig {
    /**
     * 不参与自动租户过滤的表。 sys_tenant / sys_role / sys_permission 是平台级字典； wx_user_binding
     * 需要在认证前按 openid 跨租户查找； RBAC 关联表在管理操作中可能需要跨租户查询。
     */
    private static final Set<String> IGNORED_TABLES =
            Set.of(
                    "sys_tenant",
                    "sys_role",
                    "sys_permission",
                    "sys_user_role",
                    "sys_role_permission",
                    "sys_user_workbench",
                    "sys_user_customer_scope",
                    "wx_user_binding");

    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(
                new TenantLineInnerInterceptor(
                        new com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler() {
                            @Override
                            public Expression getTenantId() {
                                // 优先使用显式设置的租户上下文（异步任务、定时扫描等场景）
                                Long explicit = TenantContext.get();
                                if (explicit != null) {
                                    return new LongValue(explicit);
                                }
                                // 其次从安全上下文获取
                                if (SecurityContextHolder.getContext().getAuthentication() != null) {
                                    return new LongValue(CurrentUser.require().tenantId());
                                }
                                // 既无显式上下文也无认证信息，说明存在编程错误
                                throw new IllegalStateException(
                                        "租户上下文缺失：既无 TenantContext 也无 SecurityContext，"
                                                + "请检查是否在异步线程或定时任务中遗漏了租户设置");
                            }

                            @Override
                            public boolean ignoreTable(String tableName) {
                                return TenantContext.isReadBypassEnabled()
                                        || IGNORED_TABLES.contains(tableName);
                            }
                        }));
        return interceptor;
    }
}
