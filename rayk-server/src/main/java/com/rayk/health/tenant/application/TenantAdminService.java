package com.rayk.health.tenant.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.system.entity.SysTenantEntity;
import com.rayk.health.system.entity.SysUserEntity;
import com.rayk.health.system.mapper.SysTenantMapper;
import com.rayk.health.system.mapper.SysUserMapper;
import com.rayk.health.system.mapper.SysUserRoleMapper;
import com.rayk.health.tenant.vo.StaffVo;
import com.rayk.health.tenant.vo.TenantProfileVo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantAdminService {
    private final SysTenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    public TenantAdminService(
            SysTenantMapper tenantMapper,
            SysUserMapper userMapper,
            SysUserRoleMapper userRoleMapper) {
        this.tenantMapper = tenantMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Transactional(readOnly = true)
    public TenantProfileVo profile() {
        CurrentPrincipal current = CurrentUser.require();
        SysTenantEntity tenant =
                tenantMapper.selectOne(
                        new LambdaQueryWrapper<SysTenantEntity>()
                                .eq(SysTenantEntity::getTenantId, current.tenantId())
                                .eq(SysTenantEntity::getDeleted, 0)
                                .last("LIMIT 1"));
        if (tenant == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        return new TenantProfileVo(
                String.valueOf(tenant.getTenantId()),
                tenant.getTenantName(),
                tenant.getStatus(),
                tenant.getServicePlan());
    }

    @Transactional(readOnly = true)
    public List<StaffVo> staff() {
        List<SysUserEntity> users =
                userMapper.selectList(
                        new LambdaQueryWrapper<SysUserEntity>()
                                .eq(SysUserEntity::getDeleted, 0)
                                .orderByAsc(SysUserEntity::getId));
        return users.stream()
                .map(
                        user ->
                                new StaffVo(
                                        String.valueOf(user.getId()),
                                        user.getUsername(),
                                        user.getDisplayName(),
                                        user.getPhoneMasked(),
                                        List.copyOf(
                                                userRoleMapper.selectRoleCodesByUserId(
                                                        user.getId())),
                                        user.getStatus()))
                .toList();
    }
}
