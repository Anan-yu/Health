package com.rayk.health.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.rayk.health.security.service.CurrentUser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class MybatisPlusConfig {
    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(
                new TenantLineInnerInterceptor(
                        new com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler() {
                            @Override
                            public Expression getTenantId() {
                                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                                    return new LongValue(0);
                                }
                                return new LongValue(CurrentUser.require().tenantId());
                            }

                            @Override
                            public boolean ignoreTable(String tableName) {
                                return tableName.equals("sys_tenant")
                                        || tableName.equals("sys_role")
                                        || tableName.equals("sys_permission")
                                        || tableName.equals("wx_user_binding");
                            }
                        }));
        return interceptor;
    }
}
