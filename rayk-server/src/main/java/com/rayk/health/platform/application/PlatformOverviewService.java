package com.rayk.health.platform.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rayk.health.platform.mapper.PlatformOverviewMapper;
import com.rayk.health.platform.vo.PlatformOverviewVo;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.platform.dto.UpdatePlatformTenantRequest;
import com.rayk.health.platform.dto.CreatePlatformTenantRequest;
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
import com.rayk.health.tenant.vo.TenantProfileVo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            PlatformOverviewMapper mapper,
            SysTenantMapper tenantMapper,
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            SysRolePermissionMapper rolePermissionMapper,
            SysUserRoleMapper userRoleMapper,
            SysUserWorkbenchMapper workbenchMapper,
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
        return new PlatformOverviewVo(
                mapper.countTenants(),
                mapper.countActiveTenants(),
                mapper.countUsers(),
                mapper.countPatients(),
                mapper.countPendingReviews(),
                mapper.countPendingFollowups(),
                mapper.selectTenants());
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

    /**
     * 平台创建机构并预录入一名机构管理员。管理员首次通过微信授权手机号登录后，
     * DatabaseUserCatalog 会按手机号哈希自动识别为该机构的 TENANT_ADMIN。
     */
    @Transactional
    public TenantProfileVo createTenant(CreatePlatformTenantRequest request) {
        String tenantCode = request.tenantCode().trim().toUpperCase(Locale.ROOT);
        if (tenantMapper.selectByTenantCodeIgnoringTenant(tenantCode) != null) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        String phone = PhoneIdentity.normalize(request.adminPhone());
        String phoneHash = PhoneIdentity.hash(phone);
        if (userMapper.selectByPhoneHashIgnoringTenant(phoneHash) != null) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
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

        SysRoleEntity adminRole = provisionStandardRoles(tenant.getTenantId(), operatorId, now);
        SysUserEntity admin = new SysUserEntity();
        admin.setId(IdWorker.getId());
        admin.setTenantId(tenant.getTenantId());
        admin.setUsername("tenant_admin_" + phoneHash.substring(0, 16));
        admin.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        admin.setDisplayName(request.adminName().trim());
        admin.setPhoneMasked(PhoneIdentity.mask(phone));
        admin.setPhoneHash(phoneHash);
        admin.setStatus("ACTIVE");
        admin.setCreatedBy(operatorId);
        admin.setCreatedAt(now);
        admin.setUpdatedBy(operatorId);
        admin.setUpdatedAt(now);
        admin.setDeleted(0);
        admin.setVersion(0);
        userMapper.insert(admin);

        SysUserRoleEntity userRole = new SysUserRoleEntity();
        userRole.setTenantId(tenant.getTenantId());
        userRole.setUserId(admin.getId());
        userRole.setRoleId(adminRole.getId());
        userRole.setCreatedBy(operatorId);
        userRole.setCreatedAt(now);
        userRole.setUpdatedBy(operatorId);
        userRole.setUpdatedAt(now);
        userRole.setDeleted(0);
        userRole.setVersion(0);
        userRoleMapper.insert(userRole);

        SysUserWorkbenchEntity workbench = new SysUserWorkbenchEntity();
        workbench.setTenantId(tenant.getTenantId());
        workbench.setUserId(admin.getId());
        workbench.setWorkbenchCode("TENANT_ADMIN");
        workbench.setIsDefault(1);
        workbench.setCreatedBy(operatorId);
        workbench.setCreatedAt(now);
        workbench.setUpdatedBy(operatorId);
        workbench.setUpdatedAt(now);
        workbench.setDeleted(0);
        workbench.setVersion(0);
        workbenchMapper.insert(workbench);
        return toProfile(tenant);
    }

    private SysRoleEntity provisionStandardRoles(long tenantId, long operatorId, LocalDateTime now) {
        List<SysRoleEntity> templates =
                roleMapper.selectList(
                        new LambdaQueryWrapper<SysRoleEntity>()
                                .eq(SysRoleEntity::getTenantId, ROLE_TEMPLATE_TENANT_ID)
                                .in(
                                        SysRoleEntity::getRoleCode,
                                        List.of("TENANT_ADMIN", "DOCTOR", "HEALTH_MANAGER", "CUSTOMER"))
                                .eq(SysRoleEntity::getStatus, "ACTIVE"));
        if (templates.size() != 4) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        SysRoleEntity adminRole = null;
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
            if ("TENANT_ADMIN".equals(role.getRoleCode())) {
                adminRole = role;
            }
        }
        if (adminRole == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return adminRole;
    }

    private void copyRolePermissions(
            long templateRoleId,
            long targetRoleId,
            long tenantId,
            long operatorId,
            LocalDateTime now) {
        List<SysRolePermissionEntity> templatePermissions =
                rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<SysRolePermissionEntity>()
                                .eq(SysRolePermissionEntity::getRoleId, templateRoleId));
        for (SysRolePermissionEntity templatePermission : templatePermissions) {
            SysRolePermissionEntity rolePermission = new SysRolePermissionEntity();
            rolePermission.setId(IdWorker.getId());
            rolePermission.setTenantId(tenantId);
            rolePermission.setRoleId(targetRoleId);
            rolePermission.setPermissionId(templatePermission.getPermissionId());
            rolePermission.setCreatedBy(operatorId);
            rolePermission.setCreatedAt(now);
            rolePermission.setUpdatedBy(operatorId);
            rolePermission.setUpdatedAt(now);
            rolePermission.setDeleted(0);
            rolePermission.setVersion(0);
            rolePermissionMapper.insert(rolePermission);
        }
    }

    private SysTenantEntity findTenant(long tenantId) {
        if (tenantId <= 1) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        SysTenantEntity tenant = tenantMapper.selectByTenantIdIgnoringTenant(tenantId);
        if (tenant == null) {
            throw new BusinessException(ErrorCode.TENANT_NOT_FOUND);
        }
        return tenant;
    }

    private TenantProfileVo toProfile(SysTenantEntity tenant) {
        return new TenantProfileVo(
                String.valueOf(tenant.getTenantId()),
                tenant.getTenantName(),
                tenant.getStatus(),
                tenant.getServicePlan());
    }
}
