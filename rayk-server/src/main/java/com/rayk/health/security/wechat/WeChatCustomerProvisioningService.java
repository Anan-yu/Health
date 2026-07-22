package com.rayk.health.security.wechat;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.patient.entity.HealthProfileEntity;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.HealthProfileMapper;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.security.service.UserAccount;
import com.rayk.health.security.service.UserCatalog;
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
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Creates the smallest possible personal account when a verified phone is not pre-registered. */
@Service
public class WeChatCustomerProvisioningService {
    private final WeChatProperties properties;
    private final SysTenantMapper tenantMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysUserWorkbenchMapper workbenchMapper;
    private final PatientMapper patientMapper;
    private final HealthProfileMapper profileMapper;
    private final UserCatalog catalog;
    private final PasswordEncoder passwordEncoder;

    public WeChatCustomerProvisioningService(
            WeChatProperties properties,
            SysTenantMapper tenantMapper,
            SysUserMapper userMapper,
            SysRoleMapper roleMapper,
            SysUserRoleMapper userRoleMapper,
            SysUserWorkbenchMapper workbenchMapper,
            PatientMapper patientMapper,
            HealthProfileMapper profileMapper,
            UserCatalog catalog,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.tenantMapper = tenantMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.workbenchMapper = workbenchMapper;
        this.patientMapper = patientMapper;
        this.profileMapper = profileMapper;
        this.catalog = catalog;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount provision(String phone) {
        String normalized = PhoneIdentity.normalize(phone);
        String phoneHash = PhoneIdentity.hash(normalized);
        UserAccount existing = catalog.findByPhoneHash(phoneHash);
        if (existing != null) {
            return existing;
        }
        SysTenantEntity tenant =
                tenantMapper.selectActiveByTenantIdIgnoringTenant(properties.defaultCustomerTenantId());
        if (tenant == null) {
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }
        SysRoleEntity customerRole =
                roleMapper.selectByTenantAndCodeIgnoringTenant(tenant.getTenantId(), "CUSTOMER");
        if (customerRole == null) {
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }
        LocalDateTime now = LocalDateTime.now();
        SysUserEntity user = new SysUserEntity();
        user.setTenantId(tenant.getTenantId());
        user.setUsername("wx_" + phoneHash.substring(0, 18));
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setDisplayName("微信用户" + normalized.substring(7));
        user.setPhoneMasked(PhoneIdentity.mask(normalized));
        user.setPhoneHash(phoneHash);
        user.setStatus("ACTIVE");
        user.setCreatedBy(0L);
        user.setCreatedAt(now);
        user.setUpdatedBy(0L);
        user.setUpdatedAt(now);
        user.setDeleted(0);
        user.setVersion(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            UserAccount concurrent = catalog.findByPhoneHash(phoneHash);
            if (concurrent != null) {
                return concurrent;
            }
            throw exception;
        }
        insertRole(tenant.getTenantId(), user.getId(), customerRole.getId(), now);
        insertWorkbench(tenant.getTenantId(), user.getId(), now);
        PatientEntity patient = new PatientEntity();
        patient.setTenantId(tenant.getTenantId());
        patient.setUserId(user.getId());
        patient.setName(user.getDisplayName());
        patient.setGender("UNKNOWN");
        patient.setPhoneMasked(user.getPhoneMasked());
        patient.setStatus("ACTIVE");
        patient.setCreatedBy(user.getId());
        patient.setCreatedAt(now);
        patient.setUpdatedBy(user.getId());
        patient.setUpdatedAt(now);
        patient.setDeleted(0);
        patient.setVersion(0);
        patientMapper.insert(patient);
        HealthProfileEntity profile = new HealthProfileEntity();
        profile.setTenantId(tenant.getTenantId());
        profile.setPatientId(patient.getId());
        profile.setProfileCompleteness(0);
        profile.setCreatedBy(user.getId());
        profile.setCreatedAt(now);
        profile.setUpdatedBy(user.getId());
        profile.setUpdatedAt(now);
        profile.setDeleted(0);
        profile.setVersion(0);
        profileMapper.insert(profile);
        return catalog.findByUserId(user.getId());
    }

    private void insertRole(long tenantId, long userId, long roleId, LocalDateTime now) {
        SysUserRoleEntity role = new SysUserRoleEntity();
        role.setTenantId(tenantId);
        role.setUserId(userId);
        role.setRoleId(roleId);
        role.setCreatedBy(userId);
        role.setCreatedAt(now);
        role.setUpdatedBy(userId);
        role.setUpdatedAt(now);
        role.setDeleted(0);
        role.setVersion(0);
        userRoleMapper.insert(role);
    }

    private void insertWorkbench(long tenantId, long userId, LocalDateTime now) {
        SysUserWorkbenchEntity workbench = new SysUserWorkbenchEntity();
        workbench.setTenantId(tenantId);
        workbench.setUserId(userId);
        workbench.setWorkbenchCode("CUSTOMER");
        workbench.setIsDefault(1);
        workbench.setCreatedBy(userId);
        workbench.setCreatedAt(now);
        workbench.setUpdatedBy(userId);
        workbench.setUpdatedAt(now);
        workbench.setDeleted(0);
        workbench.setVersion(0);
        workbenchMapper.insert(workbench);
    }
}
