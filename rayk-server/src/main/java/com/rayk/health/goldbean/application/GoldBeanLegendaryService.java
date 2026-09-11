package com.rayk.health.goldbean.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.dto.CreateGoldBeanLegendaryRequest;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanLegendaryEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanLegendaryMapper;
import com.rayk.health.goldbean.util.GoldBeanAmounts;
import com.rayk.health.goldbean.vo.GoldBeanLegendarySummaryVo;
import com.rayk.health.platform.vo.PlatformGoldBeanLegendaryVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.wechat.PhoneIdentity;
import com.rayk.health.system.entity.SysUserEntity;
import com.rayk.health.system.mapper.SysUserMapper;
import com.rayk.health.tenant.TenantContext;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Manages the platform phone allowlist and the customer-facing legendary entitlement. */
@Service
public class GoldBeanLegendaryService {
    private static final long PLATFORM_TENANT_ID = 1L;
    private static final String ACTIVE = "ACTIVE";
    private static final String REVOKED = "REVOKED";
    private static final String PAID = "PAID";

    private final GoldBeanProperties properties;
    private final GoldBeanLegendaryMapper legendaryMapper;
    private final GoldBeanAccountMapper accountMapper;
    private final SysUserMapper userMapper;

    public GoldBeanLegendaryService(
            GoldBeanProperties properties,
            GoldBeanLegendaryMapper legendaryMapper,
            GoldBeanAccountMapper accountMapper,
            SysUserMapper userMapper) {
        this.properties = properties;
        this.legendaryMapper = legendaryMapper;
        this.accountMapper = accountMapper;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public GoldBeanLegendarySummaryVo summary() {
        CurrentPrincipal current = requireCustomer();
        SysUserEntity user = userMapper.selectByIdIgnoringTenant(current.userId());
        boolean eligible = user != null
                && StringUtils.hasText(user.getPhoneHash())
                && legendaryMapper.selectActiveByPhoneHash(user.getPhoneHash()) != null;
        GoldBeanAccountEntity account = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                .eq(GoldBeanAccountEntity::getUserId, current.userId())
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1"));
        boolean registered = account != null && PAID.equals(account.getRegistrationFeeStatus())
                && "ACTIVE".equals(account.getStatus());
        // Platform direct purchase is retired. Legendary users now enter the market and
        // buy only DIGITAL_BANK listings; keep the response field for old clients.
        boolean purchaseEnabled = false;
        String message = !eligible
                ? "当前手机号尚未被平台录入传奇人物资格。"
                : !registered
                        ? "完成金豆会员注册后，才可以购买数字银行金豆。"
                        : "传奇人物仅可在金豆集市购买全国数字银行金豆挂单，不能向平台直接购买或发布挂单。";
        return new GoldBeanLegendarySummaryVo(
                eligible,
                registered,
                purchaseEnabled,
                String.valueOf(current.userId()),
                account == null
                        ? GoldBeanAmounts.ZERO
                        : GoldBeanAmounts.nonNegative(account.getDigitalBankBalance()),
                properties.goldBeanUnitPriceCent(),
                properties.maxPurchaseQuantity(),
                message);
    }

    @Transactional(readOnly = true)
    public boolean isEligible(long userId) {
        SysUserEntity user = userMapper.selectByIdIgnoringTenant(userId);
        return user != null
                && StringUtils.hasText(user.getPhoneHash())
                && legendaryMapper.selectActiveByPhoneHash(user.getPhoneHash()) != null;
    }

    public void requireEligible(long userId) {
        if (!isEligible(userId)) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_LEGENDARY_REQUIRED);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformGoldBeanLegendaryVo> list() {
        requirePlatformAdmin();
        Map<String, SysUserEntity> users = userMapper.selectAllIgnoringTenant().stream()
                .filter(item -> StringUtils.hasText(item.getPhoneHash()))
                .collect(Collectors.toMap(SysUserEntity::getPhoneHash, Function.identity(), (left, right) -> left));
        List<PlatformGoldBeanLegendaryVo> records = legendaryMapper.selectPlatformList().stream()
                .map(item -> toVo(item, users.get(item.getPhoneHash())))
                .toList();
        return PageResponse.of(records);
    }

    @Transactional
    public PlatformGoldBeanLegendaryVo create(CreateGoldBeanLegendaryRequest request) {
        CurrentPrincipal admin = requirePlatformAdmin();
        if (request == null || !StringUtils.hasText(request.phone())) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        String normalizedPhone = normalizePhone(request.phone());
        String phoneHash = PhoneIdentity.hash(normalizedPhone);
        String note = request.note() == null ? null : request.note().trim();
        LocalDateTime now = LocalDateTime.now();
        GoldBeanLegendaryEntity existing = legendaryMapper.selectByPhoneHashForUpdate(phoneHash);
        if (existing != null) {
            if (ACTIVE.equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_LEGENDARY_PHONE_CONFLICT);
            }
            existing.setPhoneMasked(PhoneIdentity.mask(normalizedPhone));
            existing.setStatus(ACTIVE);
            existing.setNote(note);
            existing.setUpdatedBy(admin.userId());
            existing.setUpdatedAt(now);
            existing.setVersion(existing.getVersion() == null ? 0 : existing.getVersion() + 1);
            legendaryMapper.updatePlatform(existing);
            return toVo(existing, findUser(phoneHash));
        }

        GoldBeanLegendaryEntity entity = new GoldBeanLegendaryEntity();
        entity.setId(IdWorker.getId());
        entity.setTenantId(PLATFORM_TENANT_ID);
        entity.setPhoneHash(phoneHash);
        entity.setPhoneMasked(PhoneIdentity.mask(normalizedPhone));
        entity.setStatus(ACTIVE);
        entity.setNote(note);
        entity.setCreatedBy(admin.userId());
        entity.setCreatedAt(now);
        entity.setUpdatedBy(admin.userId());
        entity.setUpdatedAt(now);
        entity.setDeleted(0);
        entity.setVersion(0);
        try {
            legendaryMapper.insertPlatform(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_LEGENDARY_PHONE_CONFLICT);
        }
        return toVo(entity, findUser(phoneHash));
    }

    @Transactional
    public PlatformGoldBeanLegendaryVo revoke(String idText) {
        CurrentPrincipal admin = requirePlatformAdmin();
        long id;
        try {
            id = Long.parseLong(idText == null ? "" : idText.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_LEGENDARY_NOT_FOUND);
        }
        GoldBeanLegendaryEntity existing = legendaryMapper.selectPlatformByIdForUpdate(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_LEGENDARY_NOT_FOUND);
        }
        if (!ACTIVE.equals(existing.getStatus())) return toVo(existing, findUser(existing.getPhoneHash()));
        LocalDateTime now = LocalDateTime.now();
        legendaryMapper.revokePlatform(id, admin.userId(), now);
        existing.setStatus(REVOKED);
        existing.setUpdatedBy(admin.userId());
        existing.setUpdatedAt(now);
        return toVo(existing, findUser(existing.getPhoneHash()));
    }

    private CurrentPrincipal requireCustomer() {
        if (!properties.enabled()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_NOT_ENABLED);
        }
        CurrentPrincipal current = CurrentUser.require();
        if (!"CUSTOMER".equals(current.workbench())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        return current;
    }

    private CurrentPrincipal requirePlatformAdmin() {
        if (!properties.enabled()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_NOT_ENABLED);
        }
        CurrentPrincipal current = CurrentUser.require();
        if (!current.roles().contains("PLATFORM_ADMIN")) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        return current;
    }

    private SysUserEntity findUser(String phoneHash) {
        return StringUtils.hasText(phoneHash) ? userMapper.selectByPhoneHashIgnoringTenant(phoneHash) : null;
    }

    private PlatformGoldBeanLegendaryVo toVo(GoldBeanLegendaryEntity item, SysUserEntity user) {
        GoldBeanAccountEntity account = user == null ? null : TenantContext.executeReadWithoutTenant(
                () -> accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                        .eq(GoldBeanAccountEntity::getUserId, user.getId())
                        .eq(GoldBeanAccountEntity::getDeleted, 0)
                        .last("LIMIT 1")));
        return new PlatformGoldBeanLegendaryVo(
                String.valueOf(item.getId()),
                item.getPhoneMasked(),
                item.getStatus(),
                item.getNote(),
                user == null ? null : String.valueOf(user.getId()),
                user == null ? null : user.getDisplayName(),
                account == null ? null : GoldBeanLevel.fromCode(account.getMemberLevel()).displayName(),
                account == null ? null : account.getRegistrationFeeStatus(),
                account == null ? null : safeAmount(account.getDigitalBankBalance()),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    private static BigDecimal safeAmount(BigDecimal value) {
        return GoldBeanAmounts.nonNegative(value);
    }

    private static String normalizePhone(String value) {
        try {
            return PhoneIdentity.normalize(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
    }
}
