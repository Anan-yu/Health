package com.rayk.health.security.wechat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.security.dto.AuthData;
import com.rayk.health.security.dto.WeChatBindingData;
import com.rayk.health.security.service.AuthService;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.service.MockAccount;
import com.rayk.health.security.service.MockUserCatalog;
import com.rayk.health.security.wechat.entity.WeChatUserBindingEntity;
import com.rayk.health.security.wechat.mapper.WeChatUserBindingMapper;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WeChatAuthService {
    private final WeChatCode2SessionClient code2SessionClient;
    private final WeChatProperties properties;
    private final WeChatUserBindingMapper bindingMapper;
    private final MockUserCatalog catalog;
    private final AuthService authService;

    public WeChatAuthService(
            WeChatCode2SessionClient code2SessionClient,
            WeChatProperties properties,
            WeChatUserBindingMapper bindingMapper,
            MockUserCatalog catalog,
            AuthService authService) {
        this.code2SessionClient = code2SessionClient;
        this.properties = properties;
        this.bindingMapper = bindingMapper;
        this.catalog = catalog;
        this.authService = authService;
    }

    @Transactional
    public AuthData login(String code) {
        WeChatSessionIdentity identity = code2SessionClient.exchange(code);
        WeChatUserBindingEntity binding = findByIdentity(identity);
        if (binding == null && StringUtils.hasText(properties.autoBindUsername())) {
            MockAccount account = catalog.require(properties.autoBindUsername());
            if (account != null) {
                binding = createBinding(identity, account);
            }
        }
        if (binding == null || !"ACTIVE".equals(binding.getStatus())) {
            throw new BusinessException(ErrorCode.WECHAT_ACCOUNT_NOT_BOUND);
        }
        MockAccount account = catalog.findByUserId(binding.getUserId());
        if (account == null) {
            throw new BusinessException(ErrorCode.WECHAT_ACCOUNT_NOT_BOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        binding.setLastLoginAt(now);
        binding.setUpdatedAt(now);
        binding.setUpdatedBy(account.userId());
        bindingMapper.updateById(binding);
        return authService.issue(account);
    }

    @Transactional
    public WeChatBindingData bind(String code) {
        CurrentPrincipal current = CurrentUser.require();
        MockAccount account = catalog.findByUserId(current.userId());
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
        return toData(existing);
    }

    private WeChatUserBindingEntity createBinding(
            WeChatSessionIdentity identity, MockAccount account) {
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

