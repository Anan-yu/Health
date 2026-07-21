package com.rayk.health.security.service;

import java.util.List;

/**
 * 账号目录抽象：按用户名或用户主键装配 {@link UserAccount}。
 *
 * <p>实现有两种：
 * <ul>
 *   <li>{@code MockUserCatalog}：内存固定开发账号，仅在 rayk.auth.mode=mock 时启用；</li>
 *   <li>{@code DatabaseUserCatalog}：基于 sys_user / RBAC 表，默认启用。</li>
 * </ul>
 */
public interface UserCatalog {
    /** 按用户名查找账号，找不到返回 null。登录前跨租户查找。 */
    UserAccount findByUsername(String username);

    /** 按用户主键查找账号，找不到返回 null。 */
    UserAccount findByUserId(long userId);

    /** 列出全部账号（主要用于测试与健康检查）。 */
    List<UserAccount> all();
}
