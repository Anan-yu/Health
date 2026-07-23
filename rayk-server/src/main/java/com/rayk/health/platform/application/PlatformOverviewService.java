package com.rayk.health.platform.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.platform.dto.CreatePlatformDoctorRequest;
import com.rayk.health.platform.dto.CreatePlatformTenantRequest;
import com.rayk.health.platform.dto.UpdatePlatformTenantRequest;
import com.rayk.health.platform.mapper.PlatformOverviewMapper;
import com.rayk.health.platform.vo.PlatformOverviewVo;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.wechat.PhoneIdentity;
import com.rayk.health.system.entity.SysRoleEntity;
import com.rayk.health.system.entity.SysRolePermissionEntity;
import com.rayk.health.system.entity.SysTenantEntity;
import com.rayk.health.system.entity.SysUserEntity;
import com.rayk.health.system.entity.SysUserRoleEntity;
import com.rayk.health.system.entity.SysUserWorkbenchEntity;
import com.rayk.health.system.mapper.SysRoleMapper;
import com.rayk.health.system.mapper.SysRolePermissionMapper;
import com.rayk.health.system.mapper.SysTenantMapper;
import com.rayk.health.system.mapper.SysUserMapper;
import com.rayk.health.system.mapper.SysUserRoleMapper;
import com.rayk.health.system.mapper.SysUserWorkbenchMapper;
import com.rayk.health.tenant.vo.StaffVo;
import com.rayk.health.tenant.vo.TenantProfileVo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Platform-only hospital and doctor provisioning. There is no institution-admin role. */
@Service
public class PlatformOverviewService {
    private static final long ROLE_TEMPLATE_TENANT_ID = 20001L;
    private final PlatformOverviewMapper mapper;
    private final SysTenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserWorkbenchMapper workbenchMapper;
    private final PasswordEncoder passwordEncoder;

    public PlatformOverviewService(
            PlatformOverviewMapper mapper, SysTenantMapper tenantMapper, SysUserMapper userMapper,
            SysRoleMapper roleMapper, SysRolePermissionMapper rolePermissionMapper,
            SysUserRoleMapper userRoleMapper, SysUserWorkbenchMapper workbenchMapper,
            PasswordEncoder passwordEncoder) {
        this.mapper = mapper;
        this.tenantMapper = tenantMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.workbenchMapper = workbenchMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PlatformOverviewVo overview() {
        return new PlatformOverviewVo(mapper.countTenants(), mapper.countActiveTenants(), mapper.countUsers(),
                mapper.countPatients(), 0, mapper.countPendingFollowups(), mapper.selectTenants());
    }

    @Transactional(readOnly = true)
    public TenantProfileVo tenantProfile(long tenantId) {
        return toProfile(findTenant(tenantId));
    }

    @Transactional
    public TenantProfileVo updateTenant(long tenantId, UpdatePlatformTenantRequest request) {
        SysTenantEntity tenant = findTenant(tenantId);
        tenant.setTenantName(request.tenantName().trim());
        tenant.setServicePlan(request.servicePlan().trim());
        tenant.setStatus(request.status());
        tenant.setUpdatedBy(CurrentUser.require().userId());
        tenant.setUpdatedAt(LocalDateTime.now());
        if (tenantMapper.updateProfileIgnoringTenant(tenant) != 1) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        return toProfile(tenant);
    }

    @Transactional
    public TenantProfileVo createTenant(CreatePlatformTenantRequest request) {
        String tenantCode = request.tenantCode().trim().toUpperCase(Locale.ROOT);
        if (tenantMapper.selectByTenantCodeIgnoringTenant(tenantCode) != null) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        long operatorId = CurrentUser.require().userId();
        LocalDateTime now = LocalDateTime.now();
        SysTenantEntity tenant = new SysTenantEntity();
        tenant.setId(IdWorker.getId());
        tenant.setTenantId(tenant.getId());
        tenant.setTenantCode(tenantCode);
        tenant.setTenantName(request.tenantName().trim());
        tenant.setServicePlan(request.servicePlan().trim());
        tenant.setStatus("ACTIVE");
        tenant.setCreatedBy(operatorId);
        tenant.setCreatedAt(now);
        tenant.setUpdatedBy(operatorId);
        tenant.setUpdatedAt(now);
        tenant.setDeleted(0);
        tenant.setVersion(0);
        tenantMapper.insert(tenant);
        provisionStandardRoles(tenant.getTenantId(), operatorId, now);
        return toProfile(tenant);
    }

    @Transactional(readOnly = true)
    public List<StaffVo> doctors(long tenantId) {
        findTenant(tenantId);
        return userMapper.selectDoctorsByTenantIgnoringTenant(tenantId).stream()
                .map(user -> new StaffVo(String.valueOf(user.getId()), user.getUsername(), user.getDisplayName(),
                        user.getPhoneMasked(), List.of("DOCTOR"), user.getStatus()))
                .toList();
    }

    @Transactional
    public StaffVo createDoctor(long tenantId, CreatePlatformDoctorRequest request) {
        findTenant(tenantId);
        String phone = PhoneIdentity.normalize(request.phone());
        String phoneHash = PhoneIdentity.hash(phone);
        if (userMapper.selectByPhoneHashIgnoringTenant(phoneHash) != null) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
        }
        SysRoleEntity doctorRole = roleMapper.selectByTenantAndCodeIgnoringTenant(tenantId, "DOCTOR");
        if (doctorRole == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        long operator = CurrentUser.require().userId();
        LocalDateTime now = LocalDateTime.now();
        SysUserEntity user = new SysUserEntity();
        user.setId(IdWorker.getId());
        user.setTenantId(tenantId);
        user.setUsername("doctor_" + phoneHash.substring(0, 16));
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setDisplayName(request.displayName().trim());
        user.setPhoneMasked(PhoneIdentity.mask(phone));
        user.setPhoneHash(phoneHash);
        user.setStatus("ACTIVE");
        user.setCreatedBy(operator);
        user.setCreatedAt(now);
        user.setUpdatedBy(operator);
        user.setUpdatedAt(now);
        user.setDeleted(0);
        user.setVersion(0);
        userMapper.insert(user);
        SysUserRoleEntity userRole = new SysUserRoleEntity();
        userRole.setTenantId(tenantId);
        userRole.setUserId(user.getId());
        userRole.setRoleId(doctorRole.getId());
        userRole.setCreatedBy(operator);
        userRole.setCreatedAt(now);
        userRole.setUpdatedBy(operator);
        userRole.setUpdatedAt(now);
        userRole.setDeleted(0);
        userRole.setVersion(0);
        userRoleMapper.insert(userRole);
        SysUserWorkbenchEntity workbench = new SysUserWorkbenchEntity();
        workbench.setTenantId(tenantId);
        workbench.setUserId(user.getId());
        workbench.setWorkbenchCode("DOCTOR");
        workbench.setIsDefault(1);
        workbench.setCreatedBy(operator);
        workbench.setCreatedAt(now);
        workbench.setUpdatedBy(operator);
        workbench.setUpdatedAt(now);
        workbench.setDeleted(0);
        workbench.setVersion(0);
        workbenchMapper.insert(workbench);
        return new StaffVo(String.valueOf(user.getId()), user.getUsername(), user.getDisplayName(),
                user.getPhoneMasked(), List.of("DOCTOR"), user.getStatus());
    }

    private void provisionStandardRoles(long tenantId, long operatorId, LocalDateTime now) {
        List<SysRoleEntity> templates = roleMapper.selectList(new LambdaQueryWrapper<SysRoleEntity>()
                .eq(SysRoleEntity::getTenantId, ROLE_TEMPLATE_TENANT_ID)
                .in(SysRoleEntity::getRoleCode, List.of("DOCTOR", "CUSTOMER"))
                .eq(SysRoleEntity::getStatus, "ACTIVE"));
        if (templates.size() != 2) throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        for (SysRoleEntity template : templates) {
            SysRoleEntity role = new SysRoleEntity();
            role.setId(IdWorker.getId());
            role.setTenantId(tenantId);
            role.setRoleCode(template.getRoleCode());
            role.setRoleName(template.getRoleName());
            role.setStatus("ACTIVE");
            role.setCreatedBy(operatorId);
            role.setCreatedAt(now);
            role.setUpdatedBy(operatorId);
            role.setUpdatedAt(now);
            role.setDeleted(0);
            role.setVersion(0);
            roleMapper.insert(role);
            copyRolePermissions(template.getId(), role.getId(), tenantId, operatorId, now);
        }
    }

    private void copyRolePermissions(long sourceRoleId, long targetRoleId, long tenantId, long operatorId,
            LocalDateTime now) {
        for (SysRolePermissionEntity source : rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermissionEntity>().eq(SysRolePermissionEntity::getRoleId, sourceRoleId))) {
            SysRolePermissionEntity permission = new SysRolePermissionEntity();
            permission.setId(IdWorker.getId()); permission.setTenantId(tenantId); permission.setRoleId(targetRoleId);
            permission.setPermissionId(source.getPermissionId()); permission.setCreatedBy(operatorId);
            permission.setCreatedAt(now); permission.setUpdatedBy(operatorId); permission.setUpdatedAt(now);
            permission.setDeleted(0); permission.setVersion(0); rolePermissionMapper.insert(permission);
        }
    }

    private SysTenantEntity findTenant(long tenantId) {
        if (tenantId <= 1) throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        SysTenantEntity tenant = tenantMapper.selectByTenantIdIgnoringTenant(tenantId);
        if (tenant == null) throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        return tenant;
    }

    private TenantProfileVo toProfile(SysTenantEntity tenant) {
        return new TenantProfileVo(String.valueOf(tenant.getTenantId()), tenant.getTenantName(), tenant.getStatus(), tenant.getServicePlan());
    }
}
