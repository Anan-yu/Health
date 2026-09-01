package com.rayk.health.security.wechat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.security.dto.AuthData;
import com.rayk.health.security.dto.WeChatBindingData;
import com.rayk.health.security.service.AuthService;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.service.UserAccount;
import com.rayk.health.security.service.UserCatalog;
import com.rayk.health.security.wechat.entity.WeChatUserBindingEntity;
import com.rayk.health.security.wechat.mapper.WeChatUserBindingMapper;
import com.rayk.health.system.mapper.SysUserMapper;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WeChatAuthService {
    private static final Logger log = LoggerFactory.getLogger(WeChatAuthService.class);

    private final WeChatCode2SessionClient code2SessionClient;
    private final WeChatPhoneNumberClient phoneNumberClient;
    private final WeChatCustomerProvisioningService customerProvisioningService;
    private final WeChatStaffInviteService staffInviteService;
    private final WeChatProperties properties;
    private final WeChatUserBindingMapper bindingMapper;
    private final WeChatSessionKeyStore sessionKeyStore;
    private final UserCatalog catalog;
    private final AuthService authService;
    private final SysUserMapper userMapper;

    public WeChatAuthService(
            WeChatCode2SessionClient code2SessionClient,
            WeChatPhoneNumberClient phoneNumberClient,
            WeChatCustomerProvisioningService customerProvisioningService,
            WeChatStaffInviteService staffInviteService,
            WeChatProperties properties,
            WeChatUserBindingMapper bindingMapper,
            WeChatSessionKeyStore sessionKeyStore,
            UserCatalog catalog,
            AuthService authService,
            SysUserMapper userMapper) {
        this.code2SessionClient = code2SessionClient;
        this.phoneNumberClient = phoneNumberClient;
        this.customerProvisioningService = customerProvisioningService;
        this.staffInviteService = staffInviteService;
        this.properties = properties;
        this.bindingMapper = bindingMapper;
        this.sessionKeyStore = sessionKeyStore;
        this.catalog = catalog;
        this.authService = authService;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthData login(String code, String phoneCode) {
        boolean hasPhoneCode = StringUtils.hasText(phoneCode);
        if (properties.phoneLoginRequired() && !hasPhoneCode) {
            log.warn(
                    "WeChat login rejected before phone verification: hasPhoneCode=false, "
                            + "phoneLoginRequired=true");
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }
        // A real getPhoneNumber credential is authoritative, even when the
        // local development profile keeps mock login enabled for H5/debug
        // accounts.  Using the shared mock OpenID here would reuse whichever
        // role logged in first and could incorrectly turn a pre-registered
        // platform administrator into a CUSTOMER.
        boolean verifiedPhoneLogin = hasPhoneCode;
        WeChatSessionIdentity identity = verifiedPhoneLogin
                ? code2SessionClient.exchangeReal(code)
                : code2SessionClient.exchange(code);
        String verifiedPhone = null;
        UserAccount phoneAccount = null;
        if (hasPhoneCode || properties.mockEnabled()) {
            verifiedPhone = verifiedPhoneLogin
                    ? phoneNumberClient.resolveReal(phoneCode)
                    : phoneNumberClient.resolve(phoneCode);
            phoneAccount = catalog.findByPhoneHash(PhoneIdentity.hash(verifiedPhone));
            if (phoneAccount == null && matchesPlatformAdminBootstrap(verifiedPhone)) {
                UserAccount configuredAdmin = catalog.findByUsername(properties.platformAdminUsername().trim());
                if (configuredAdmin != null && configuredAdmin.roles().contains("PLATFORM_ADMIN")) {
                    phoneAccount = configuredAdmin;
                    userMapper.updatePhoneIdentityIgnoringTenant(
                            configuredAdmin.userId(),
                            PhoneIdentity.mask(verifiedPhone),
                            PhoneIdentity.hash(verifiedPhone),
                            configuredAdmin.userId(),
                            LocalDateTime.now());
                }
            }
        }
        WeChatUserBindingEntity binding = findByIdentity(identity);
        if (binding != null && verifiedPhoneLogin && phoneAccount == null) {
            // A verified phone is authoritative.  If it is not a pre-registered
            // doctor/admin phone, provision a CUSTOMER and migrate any stale
            // OpenID binding instead of returning the old role or requiring a
            // manual bind.  This also makes a retired doctor phone become a new
            // ordinary customer identity on the next login.
            phoneAccount = customerProvisioningService.provision(verifiedPhone);
        }
        if (binding != null && phoneAccount != null && binding.getUserId() != phoneAccount.userId()) {
            // A verified phone is the authoritative identity for enterprise login. A
            // previous customer login may have left this OpenID bound to CUSTOMER;
            // migrate that stale binding to the verified staff account instead of
            // silently returning the old role. Never replace an existing binding of
            // the staff account to a different OpenID.
            WeChatUserBindingEntity staffBinding = findByUser(identity.appId(), phoneAccount.userId());
            if (staffBinding != null && !identity.openid().equals(staffBinding.getOpenid())) {
                throw new BusinessException(ErrorCode.WECHAT_ALREADY_BOUND);
            }
            LocalDateTime now = LocalDateTime.now();
            binding.setTenantId(phoneAccount.tenantId());
            binding.setUserId(phoneAccount.userId());
            binding.setStatus("ACTIVE");
            binding.setUpdatedBy(phoneAccount.userId());
            binding.setUpdatedAt(now);
            bindingMapper.updateById(binding);
        }
        if (binding == null) {
            UserAccount account = null;
            if (properties.mockEnabled() && StringUtils.hasText(properties.autoBindUsername())) {
                account = catalog.findByUsername(properties.autoBindUsername());
            }
            if (account == null) {
                if (verifiedPhone != null) {
                    account = phoneAccount;
                    if (account == null) {
                        account = customerProvisioningService.provision(verifiedPhone);
                    }
                } else if (!properties.phoneLoginRequired()) {
                    // Compatibility path for personal-subject development apps that do not
                    // expose the phone fast-verification component.
                    account = customerProvisioningService.provision(identity);
                }
            }
            binding = createBinding(identity, account);
        }
        if (binding == null || !"ACTIVE".equals(binding.getStatus())) {
            throw new BusinessException(ErrorCode.WECHAT_ACCOUNT_NOT_BOUND);
        }
        UserAccount account = catalog.findByUserId(binding.getUserId());
        if (account == null || !account.isActive()) {
            throw new BusinessException(ErrorCode.WECHAT_ACCOUNT_NOT_BOUND);
        }
        sessionKeyStore.save(identity, account.userId());
        LocalDateTime now = LocalDateTime.now();
        binding.setLastLoginAt(now);
        binding.setUpdatedAt(now);
        binding.setUpdatedBy(account.userId());
        bindingMapper.updateById(binding);
        return authService.issue(account);
    }

    @Transactional
    public AuthData loginWithStaffInvite(String code, String inviteCode) {
        if (properties.phoneLoginRequired()) {
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }
        WeChatSessionIdentity identity = code2SessionClient.exchange(code);
        WeChatUserBindingEntity existingBinding = findByIdentity(identity);
        if (existingBinding != null) {
            if (!"ACTIVE".equals(existingBinding.getStatus())) {
                throw new BusinessException(ErrorCode.WECHAT_ACCOUNT_NOT_BOUND);
            }
            UserAccount bound = catalog.findByUserId(existingBinding.getUserId());
            if (bound == null || !bound.isActive()
                    || (!bound.roles().contains("DOCTOR") && !bound.roles().contains("PLATFORM_ADMIN"))) {
                throw new BusinessException(ErrorCode.WECHAT_ALREADY_BOUND);
            }
            sessionKeyStore.save(identity, bound.userId());
            return authService.issue(bound);
        }

        UserAccount account = catalog.findByUserId(staffInviteService.consume(inviteCode));
        if (account == null || !account.isActive()
                || (!account.roles().contains("DOCTOR") && !account.roles().contains("PLATFORM_ADMIN"))) {
            throw new BusinessException(ErrorCode.WECHAT_ACCOUNT_NOT_BOUND);
        }
        WeChatUserBindingEntity existingUserBinding = findByUser(identity.appId(), account.userId());
        if (existingUserBinding != null && !identity.openid().equals(existingUserBinding.getOpenid())) {
            throw new BusinessException(ErrorCode.WECHAT_ALREADY_BOUND);
        }
        if (existingUserBinding == null) {
            createBinding(identity, account);
        }
        sessionKeyStore.save(identity, account.userId());
        return authService.issue(account);
    }

    @Transactional
    public AuthData loginWithPlatformAdminPassword(
            String code, String username, String password) {
        if (properties.phoneLoginRequired()) {
            throw new BusinessException(ErrorCode.WECHAT_PHONE_AUTH_FAILED);
        }
        WeChatSessionIdentity identity = code2SessionClient.exchange(code);
        UserAccount account = authService.authenticate(username.trim(), password);
        if (!account.roles().contains("PLATFORM_ADMIN")) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }

        WeChatUserBindingEntity occupied = findByIdentity(identity);
        if (occupied != null && !occupied.getUserId().equals(account.userId())) {
            throw new BusinessException(ErrorCode.WECHAT_ALREADY_BOUND);
        }
        WeChatUserBindingEntity existing = findByUser(identity.appId(), account.userId());
        if (existing != null && !identity.openid().equals(existing.getOpenid())) {
            throw new BusinessException(ErrorCode.WECHAT_ALREADY_BOUND);
        }
        if (existing == null) {
            createBinding(identity, account);
        } else if (!"ACTIVE".equals(existing.getStatus())) {
            existing.setStatus("ACTIVE");
            existing.setUpdatedBy(account.userId());
            existing.setUpdatedAt(LocalDateTime.now());
            bindingMapper.updateById(existing);
        }
        sessionKeyStore.save(identity, account.userId());
        return authService.issue(account);
    }

    @Transactional
    public WeChatBindingData bind(String code) {
        CurrentPrincipal current = CurrentUser.require();
        UserAccount account = catalog.findByUserId(current.userId());
        if (account == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        WeChatSessionIdentity identity = code2SessionClient.exchange(code);
        WeChatUserBindingEntity occupied = findByIdentity(identity);
        if (occupied != null && !occupied.getUserId().equals(current.userId())) {
            throw new BusinessException(ErrorCode.WECHAT_ALREADY_BOUND);
        }
        WeChatUserBindingEntity existing = findByUser(identity.appId(), current.userId());
        if (existing == null) {
            existing = createBinding(identity, account);
        } else {
            existing.setOpenid(identity.openid());
            existing.setUnionid(identity.unionid());
            existing.setStatus("ACTIVE");
            existing.setUpdatedBy(current.userId());
            existing.setUpdatedAt(LocalDateTime.now());
            bindingMapper.updateById(existing);
        }
        sessionKeyStore.save(identity, current.userId());
        return toData(existing);
    }

    private WeChatUserBindingEntity createBinding(
            WeChatSessionIdentity identity, UserAccount account) {
        WeChatUserBindingEntity binding = new WeChatUserBindingEntity();
        LocalDateTime now = LocalDateTime.now();
        binding.setTenantId(account.tenantId());
        binding.setUserId(account.userId());
        binding.setAppId(identity.appId());
        binding.setOpenid(identity.openid());
        binding.setUnionid(identity.unionid());
        binding.setStatus("ACTIVE");
        binding.setLastLoginAt(now);
        binding.setCreatedBy(account.userId());
        binding.setCreatedAt(now);
        binding.setUpdatedBy(account.userId());
        binding.setUpdatedAt(now);
        binding.setDeleted(0);
        binding.setVersion(0);
        try {
            bindingMapper.insert(binding);
            return binding;
        } catch (DuplicateKeyException exception) {
            WeChatUserBindingEntity concurrent = findByIdentity(identity);
            if (concurrent != null && concurrent.getUserId().equals(account.userId())) {
                return concurrent;
            }
            throw new BusinessException(ErrorCode.WECHAT_ALREADY_BOUND);
        }
    }

    private WeChatUserBindingEntity findByIdentity(WeChatSessionIdentity identity) {
        return bindingMapper.selectOne(
                new LambdaQueryWrapper<WeChatUserBindingEntity>()
                        .eq(WeChatUserBindingEntity::getAppId, identity.appId())
                        .eq(WeChatUserBindingEntity::getOpenid, identity.openid())
                        .eq(WeChatUserBindingEntity::getDeleted, 0));
    }

    private WeChatUserBindingEntity findByUser(String appId, long userId) {
        return bindingMapper.selectOne(
                new LambdaQueryWrapper<WeChatUserBindingEntity>()
                        .eq(WeChatUserBindingEntity::getAppId, appId)
                        .eq(WeChatUserBindingEntity::getUserId, userId)
                        .eq(WeChatUserBindingEntity::getDeleted, 0));
    }

    private boolean matchesPlatformAdminBootstrap(String verifiedPhone) {
        if (!StringUtils.hasText(properties.platformAdminPhone())
                || !StringUtils.hasText(properties.platformAdminUsername())) {
            return false;
        }
        try {
            return PhoneIdentity.normalize(verifiedPhone)
                    .equals(PhoneIdentity.normalize(properties.platformAdminPhone()));
        } catch (IllegalArgumentException exception) {
            // A malformed deployment secret must not make all WeChat logins fail.
            return false;
        }
    }

    private WeChatBindingData toData(WeChatUserBindingEntity binding) {
        String openid = binding.getOpenid();
        String masked = openid.length() <= 8 ? "****" : openid.substring(0, 4) + "****" + openid.substring(openid.length() - 4);
        return new WeChatBindingData(
                String.valueOf(binding.getUserId()),
                binding.getAppId(),
                masked,
                binding.getStatus(),
                binding.getCreatedAt());
    }
}
