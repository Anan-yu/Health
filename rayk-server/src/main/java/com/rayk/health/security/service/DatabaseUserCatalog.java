package com.rayk.health.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.system.entity.SysTenantEntity;
import com.rayk.health.system.entity.SysUserEntity;
import com.rayk.health.system.entity.SysUserWorkbenchEntity;
import com.rayk.health.system.mapper.SysRolePermissionMapper;
import com.rayk.health.system.mapper.SysTenantMapper;
import com.rayk.health.system.mapper.SysUserMapper;
import com.rayk.health.system.mapper.SysUserRoleMapper;
import com.rayk.health.system.mapper.SysUserWorkbenchMapper;
import java.util.Comparator;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 基于数据库 RBAC 表的账号目录实现（默认启用）。
 *
 * <p>登录前无租户上下文，{@link SysUserMapper} 的按用户名/主键查询通过
 * {@code @InterceptorIgnore} 显式跨租户；角色、权限、工作台关联表本身已被
 * 租户拦截器忽略，可直接按 user_id 聚合。
 */
@Service
@ConditionalOnProperty(name = "rayk.auth.mode", havingValue = "database", matchIfMissing = true)
public class DatabaseUserCatalog implements UserCatalog {
    private final SysUserMapper userMapper;
    private final SysTenantMapper tenantMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserWorkbenchMapper userWorkbenchMapper;

    public DatabaseUserCatalog(
            SysUserMapper userMapper,
            SysTenantMapper tenantMapper,
            SysUserRoleMapper userRoleMapper,
            SysRolePermissionMapper rolePermissionMapper,
            SysUserWorkbenchMapper userWorkbenchMapper) {
        this.userMapper = userMapper;
        this.tenantMapper = tenantMapper;
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userWorkbenchMapper = userWorkbenchMapper;
    }

    @Override
    public UserAccount findByUsername(String username) {
        SysUserEntity user = userMapper.selectByUsernameIgnoringTenant(username);
        return user == null ? null : assemble(user);
    }

    @Override
    public UserAccount findByUserId(long userId) {
        SysUserEntity user = userMapper.selectByIdIgnoringTenant(userId);
        return user == null ? null : assemble(user);
    }

    @Override
    public UserAccount findByPhoneHash(String phoneHash) {
        SysUserEntity user = userMapper.selectByPhoneHashIgnoringTenant(phoneHash);
        return user == null ? null : assemble(user);
    }

    @Override
    public List<UserAccount> all() {
        return userMapper.selectAllIgnoringTenant().stream().map(this::assemble).toList();
    }

    private UserAccount assemble(SysUserEntity user) {
        long userId = user.getId();
        List<String> roles = userRoleMapper.selectRoleCodesByUserId(userId);
        List<String> permissions = rolePermissionMapper.selectPermissionCodesByUserId(userId);
        List<SysUserWorkbenchEntity> workbenchEntities =
                userWorkbenchMapper.selectList(
                        new LambdaQueryWrapper<SysUserWorkbenchEntity>()
                                .eq(SysUserWorkbenchEntity::getUserId, userId)
                                .eq(SysUserWorkbenchEntity::getDeleted, 0));
        List<WorkbenchOption> workbenches =
                workbenchEntities.stream()
                        .sorted(
                                Comparator.comparing(
                                                SysUserWorkbenchEntity::getIsDefault,
                                                Comparator.nullsLast(Comparator.reverseOrder()))
                                        .thenComparing(SysUserWorkbenchEntity::getId))
                        .map(
                                entity ->
                                        new WorkbenchOption(
                                                entity.getWorkbenchCode(),
                                                WorkbenchCatalog.displayName(
                                                        entity.getWorkbenchCode())))
                        .toList();
        String defaultWorkbench =
                workbenchEntities.stream()
                        .filter(entity -> Integer.valueOf(1).equals(entity.getIsDefault()))
                        .map(SysUserWorkbenchEntity::getWorkbenchCode)
                        .findFirst()
                        .orElse(workbenches.isEmpty() ? null : workbenches.getFirst().code());
        return new UserAccount(
                userId,
                user.getTenantId(),
                tenantName(user.getTenantId()),
                user.getUsername(),
                user.getDisplayName(),
                user.getPasswordHash(),
                user.getStatus(),
                List.copyOf(roles),
                List.copyOf(permissions),
                workbenches,
                defaultWorkbench);
    }

    private String tenantName(long tenantId) {
        SysTenantEntity tenant =
                tenantMapper.selectOne(
                        new LambdaQueryWrapper<SysTenantEntity>()
                                .eq(SysTenantEntity::getTenantId, tenantId)
                                .eq(SysTenantEntity::getDeleted, 0)
                                .last("LIMIT 1"));
        return tenant == null ? null : tenant.getTenantName();
    }
}
