package com.rayk.health.tenant.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.wechat.PhoneIdentity;
import com.rayk.health.system.entity.SysRoleEntity;
import com.rayk.health.system.entity.SysTenantEntity;
import com.rayk.health.system.entity.SysUserEntity;
import com.rayk.health.system.entity.SysUserRoleEntity;
import com.rayk.health.system.entity.SysUserWorkbenchEntity;
import com.rayk.health.system.mapper.SysRoleMapper;
import com.rayk.health.system.mapper.SysTenantMapper;
import com.rayk.health.system.mapper.SysUserMapper;
import com.rayk.health.system.mapper.SysUserRoleMapper;
import com.rayk.health.system.mapper.SysUserWorkbenchMapper;
import com.rayk.health.tenant.dto.CreateStaffRequest;
import com.rayk.health.tenant.vo.StaffVo;
import com.rayk.health.tenant.vo.TenantProfileVo;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantAdminService {
    private final SysTenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserWorkbenchMapper workbenchMapper;
    private final PasswordEncoder passwordEncoder;

    public TenantAdminService(
            SysTenantMapper tenantMapper,
            SysUserMapper userMapper,
            SysUserRoleMapper userRoleMapper,
            SysRoleMapper roleMapper,
            SysUserWorkbenchMapper workbenchMapper,
            PasswordEncoder passwordEncoder) {
        this.tenantMapper = tenantMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.workbenchMapper = workbenchMapper;
        this.passwordEncoder = passwordEncoder;
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

    @Transactional
    public StaffVo createStaff(CreateStaffRequest request) {
        CurrentPrincipal current = CurrentUser.require();
        String phone = PhoneIdentity.normalize(request.phone());
        String phoneHash = PhoneIdentity.hash(phone);
        if (userMapper.selectByPhoneHashIgnoringTenant(phoneHash) != null) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }
        SysRoleEntity role =
                roleMapper.selectByTenantAndCodeIgnoringTenant(current.tenantId(), request.roleCode());
        if (role == null) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        LocalDateTime now = LocalDateTime.now();
        SysUserEntity user = new SysUserEntity();
        user.setTenantId(current.tenantId());
        user.setUsername("staff_" + phoneHash.substring(0, 18));
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setDisplayName(request.displayName().trim());
        user.setPhoneMasked(PhoneIdentity.mask(phone));
        user.setPhoneHash(phoneHash);
        user.setStatus("ACTIVE");
        user.setCreatedBy(current.userId());
        user.setCreatedAt(now);
        user.setUpdatedBy(current.userId());
        user.setUpdatedAt(now);
        user.setDeleted(0);
        user.setVersion(0);
        userMapper.insert(user);

        SysUserRoleEntity userRole = new SysUserRoleEntity();
        userRole.setTenantId(current.tenantId());
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRole.setCreatedBy(current.userId());
        userRole.setCreatedAt(now);
        userRole.setUpdatedBy(current.userId());
        userRole.setUpdatedAt(now);
        userRole.setDeleted(0);
        userRole.setVersion(0);
        userRoleMapper.insert(userRole);

        SysUserWorkbenchEntity workbench = new SysUserWorkbenchEntity();
        workbench.setTenantId(current.tenantId());
        workbench.setUserId(user.getId());
        workbench.setWorkbenchCode(request.roleCode());
        workbench.setIsDefault(1);
        workbench.setCreatedBy(current.userId());
        workbench.setCreatedAt(now);
        workbench.setUpdatedBy(current.userId());
        workbench.setUpdatedAt(now);
        workbench.setDeleted(0);
        workbench.setVersion(0);
        workbenchMapper.insert(workbench);
        return new StaffVo(
                String.valueOf(user.getId()),
                user.getUsername(),
                user.getDisplayName(),
                user.getPhoneMasked(),
                List.of(request.roleCode()),
                user.getStatus());
    }
}
