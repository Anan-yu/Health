package com.rayk.health.goldbean.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rayk.health.common.util.TextEncodingUtils;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.dto.OpenGoldRegionRequest;
import com.rayk.health.goldbean.dto.RegisterGoldBeanRequest;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanLedgerEntity;
import com.rayk.health.goldbean.entity.GoldBeanOrderEntity;
import com.rayk.health.goldbean.entity.GoldBeanPlatformInviteEntity;
import com.rayk.health.goldbean.entity.GoldBeanReferralEntity;
import com.rayk.health.goldbean.entity.GoldRegionEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanLedgerMapper;
import com.rayk.health.goldbean.mapper.GoldBeanOrderMapper;
import com.rayk.health.goldbean.mapper.GoldBeanReferralMapper;
import com.rayk.health.goldbean.mapper.GoldRegionMapper;
import com.rayk.health.goldbean.util.GoldBeanAmounts;
import com.rayk.health.goldbean.vo.GoldBeanLedgerVo;
import com.rayk.health.goldbean.vo.GoldBeanSummaryVo;
import com.rayk.health.goldbean.vo.GoldRegionVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.tenant.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Implementation of the gold-bean membership rules.
 *
 * <p>All balances are derived from an append-only ledger and the account row is updated in the
 * same transaction. This service intentionally has no withdrawal or cash-out operation.
 */
@Service
public class GoldBeanApplicationService {
    private static final int DIRECT_REFERRAL_REWARD = 11;
    private static final int DOWNLINE_REFERRAL_REWARD = 5;
    private static final String PLATFORM = "PLATFORM";
    private static final String REFERRER = "REFERRER";

    private final GoldBeanProperties properties;
    private final GoldBeanAccountMapper accountMapper;
    private final GoldBeanReferralMapper referralMapper;
    private final GoldBeanLedgerMapper ledgerMapper;
    private final GoldBeanOrderMapper orderMapper;
    private final GoldRegionMapper regionMapper;
    private final GoldBeanPlatformInviteService platformInviteService;
    private final GoldRegionProfitService regionProfitService;

    public GoldBeanApplicationService(
            GoldBeanProperties properties,
            GoldBeanAccountMapper accountMapper,
            GoldBeanReferralMapper referralMapper,
            GoldBeanLedgerMapper ledgerMapper,
            GoldBeanOrderMapper orderMapper,
            GoldRegionMapper regionMapper,
            GoldBeanPlatformInviteService platformInviteService) {
        this(properties, accountMapper, referralMapper, ledgerMapper, orderMapper, regionMapper,
                platformInviteService, null);
    }

    @Autowired
    public GoldBeanApplicationService(
            GoldBeanProperties properties,
            GoldBeanAccountMapper accountMapper,
            GoldBeanReferralMapper referralMapper,
            GoldBeanLedgerMapper ledgerMapper,
            GoldBeanOrderMapper orderMapper,
            GoldRegionMapper regionMapper,
            GoldBeanPlatformInviteService platformInviteService,
            @Lazy GoldRegionProfitService regionProfitService) {
        this.properties = properties;
        this.accountMapper = accountMapper;
        this.referralMapper = referralMapper;
        this.ledgerMapper = ledgerMapper;
        this.orderMapper = orderMapper;
        this.regionMapper = regionMapper;
        this.platformInviteService = platformInviteService;
        this.regionProfitService = regionProfitService;
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public GoldBeanSummaryVo summary() {
        CurrentPrincipal current = requireEnabledCustomer();
        GoldBeanAccountEntity account = ensureAccount(current);
        settle(account, current);
        return toSummary(account, current);
    }

    /**
     * Completes a record-only registration when the explicit development switch is enabled.
     * Payment-backed registrations use the order/payment path instead.
     */
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public GoldBeanSummaryVo register(RegisterGoldBeanRequest request) {
        CurrentPrincipal current = requireEnabledCustomer();
        if (properties.paymentEnabled() || !properties.developmentMode()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_REQUIRED);
        }
        GoldBeanAccountEntity account = ensureAccount(current);
        settle(account, current);
        if ("PAID".equals(account.getRegistrationFeeStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_ALREADY_REGISTERED);
        }

        String referralCode = request == null || request.referralCode() == null
                ? ""
                : request.referralCode().trim().toUpperCase(Locale.ROOT);
        String platformInviteCode = request == null || request.platformInviteCode() == null
                ? ""
                : request.platformInviteCode().trim().toUpperCase(Locale.ROOT);
        if (!referralCode.isBlank() && !platformInviteCode.isBlank()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REGISTRATION_CODE_CONFLICT);
        }
        GoldBeanAccountEntity referrer = null;
        String recipient = PLATFORM;
        GoldBeanPlatformInviteEntity platformInvite = null;
        String developmentOrderNo = "GBD" + IdWorker.getId();
        if (!referralCode.isBlank()) {
            referrer = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                    .eq(GoldBeanAccountEntity::getReferralCode, referralCode)
                    .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                    .eq(GoldBeanAccountEntity::getStatus, "ACTIVE")
                    .eq(GoldBeanAccountEntity::getRegistrationFeeStatus, "PAID")
                    .eq(GoldBeanAccountEntity::getDeleted, 0)
                    .last("LIMIT 1 FOR UPDATE"));
            if (referrer == null || Objects.equals(referrer.getUserId(), account.getUserId())) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_INVALID);
            }
            recipient = REFERRER;
        } else if (hasPlatformRegistration()) {
            // Only the first platform-wide registration may omit a direct referrer.
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_REGISTRATION_ONLY);
        } else {
            platformInvite = platformInviteService.reserve(
                    platformInviteCode, current, developmentOrderNo);
        }

        String requestedCity = TextEncodingUtils.repairUtf8Mojibake(
                request == null || request.city() == null ? "" : request.city().trim());
        if (!requestedCity.isBlank()) {
            if (StringUtils.hasText(account.getCity()) && !requestedCity.equals(account.getCity())) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_INVALID);
            }
            account.setCity(requestedCity);
        }
        LocalDateTime now = LocalDateTime.now();
        GoldBeanOrderEntity developmentOrder = newDevelopmentRegistrationOrder(
                current, developmentOrderNo, platformInvite, referralCode, referrer, recipient, requestedCity, now);
        try {
            orderMapper.insert(developmentOrder);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_REGISTRATION_ONLY);
        }
        account.setRegistrationFeeStatus("PAID");
        account.setRegistrationFeeRecipient(recipient);
        account.setRegistrationFeeCent(registrationFeeCent(recipient));
        account.setRegistrationFeePaidAt(now);
        if (!StringUtils.hasText(account.getReferralCode())) {
            account.setReferralCode(createReferralCode(account.getUserId()));
        }
        if (referrer != null) {
            account.setReferrerId(referrer.getUserId());
        }
        // Customer accounts are provisioned before membership registration so the client can
        // display onboarding state. The registration date is the first daily-reward day.
        account.setDailyRewardStartAt(now);
        touch(account, current.userId(), now);
        accountMapper.updateById(account);
        // 注册成功当天只发放第 1 天的每日奖励（60 金豆）；后续结算通过幂等键续发。
        accrueDailyRewards(account, current.userId());
        if (platformInvite != null) {
            platformInviteService.consume(platformInvite.getId(), developmentOrderNo, current.userId());
        }

        if (referrer != null) {
            GoldBeanReferralEntity relation = new GoldBeanReferralEntity();
            relation.setId(IdWorker.getId());
            relation.setTenantId(current.tenantId());
            relation.setReferrerId(referrer.getUserId());
            relation.setReferredId(account.getUserId());
            relation.setReferralCode(referralCode);
            relation.setRegistrationFeeCent(properties.registrationFeeCent());
            relation.setRegistrationFeeRecipient(REFERRER);
            relation.setStatus("ACTIVE");
            relation.setRegisteredAt(now);
            auditNew(relation, current.userId(), now);
            try {
                referralMapper.insert(relation);
            } catch (DuplicateKeyException exception) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_ALREADY_REGISTERED);
            }
            processReferralReward(referrer, account, relation, now);
        }
        return toSummary(account, current);
    }

    boolean hasPlatformRegistration() {
        // A legacy record-only demo registration can have the same account flags as a paid
        // registration. Only a successfully delivered virtual-payment order reserves the
        // platform's first-member slot.
        return TenantContext.executeReadWithoutTenant(() -> orderMapper.selectCount(
                new LambdaQueryWrapper<GoldBeanOrderEntity>()
                        .ne(GoldBeanOrderEntity::getTenantId, 1L)
                        .eq(GoldBeanOrderEntity::getOrderType, "REGISTRATION_FEE")
                        .eq(GoldBeanOrderEntity::getStatus, "PAID")
                        .eq(GoldBeanOrderEntity::getRegistrationFeeRecipient, PLATFORM)
                        .in(GoldBeanOrderEntity::getPaymentChannel, "WECHAT_VIRTUAL", "DEVELOPMENT_RECORD")
                        .eq(GoldBeanOrderEntity::getDeleted, 0)) > 0);
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional(readOnly = true)
    public List<GoldBeanLedgerVo> ledger() {
        CurrentPrincipal current = requireEnabledCustomer();
        GoldBeanAccountEntity account = requiredAccount(current);
        return ledgerMapper.selectList(new LambdaQueryWrapper<GoldBeanLedgerEntity>()
                        .eq(GoldBeanLedgerEntity::getTenantId, current.tenantId())
                        .eq(GoldBeanLedgerEntity::getUserId, account.getUserId())
                        .eq(GoldBeanLedgerEntity::getDeleted, 0)
                        .orderByDesc(GoldBeanLedgerEntity::getCreatedAt)
                        .last("LIMIT 100"))
                .stream()
                .map(item -> new GoldBeanLedgerVo(
                        String.valueOf(item.getId()),
                        item.getBucket(),
                        item.getDirection(),
                        item.getAmount(),
                        item.getEventType(),
                        item.getDescription(),
                        item.getCreatedAt()))
                .toList();
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public GoldRegionVo openRegion(OpenGoldRegionRequest request) {
        CurrentPrincipal current = requireEnabledCustomer();
        GoldBeanAccountEntity account = ensureAccount(current);
        settle(account, current);
        if (!GoldBeanLevel.DIAMOND.code().equals(account.getMemberLevel())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_LEVEL_REQUIRED);
        }
        List<GoldBeanAccountEntity> ancestors = loadRegionAncestors(account, true);
        if (ancestors.size() > 2) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REGION_DEPTH_EXCEEDED);
        }
        if (regionMapper.selectOne(new LambdaQueryWrapper<GoldRegionEntity>()
                .eq(GoldRegionEntity::getTenantId, current.tenantId())
                .eq(GoldRegionEntity::getOwnerUserId, current.userId())
                .eq(GoldRegionEntity::getDeleted, 0)
                .last("LIMIT 1")) != null) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REGION_CONFLICT);
        }
        String city = TextEncodingUtils.repairUtf8Mojibake(
                request == null || request.city() == null ? "" : request.city().trim());
        if (city.isBlank()) throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        if (regionMapper.selectOne(new LambdaQueryWrapper<GoldRegionEntity>()
                .eq(GoldRegionEntity::getTenantId, current.tenantId())
                .eq(GoldRegionEntity::getCity, city)
                .eq(GoldRegionEntity::getDeleted, 0)
                .last("LIMIT 1")) != null) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REGION_CONFLICT);
        }

        GoldRegionEntity parent = null;
        if (!ancestors.isEmpty()) {
            parent = regionMapper.selectOne(new LambdaQueryWrapper<GoldRegionEntity>()
                    .eq(GoldRegionEntity::getTenantId, current.tenantId())
                    .eq(GoldRegionEntity::getOwnerUserId, ancestors.get(0).getUserId())
                    .eq(GoldRegionEntity::getDeleted, 0)
                    .last("LIMIT 1 FOR UPDATE"));
            if (parent != null && (parent.getDepth() == null || parent.getDepth() >= 3)) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_REGION_DEPTH_EXCEEDED);
            }
        }
        String parentText = request == null || request.parentRegionId() == null
                ? ""
                : request.parentRegionId().trim();
        if (!parentText.isBlank()) {
            if (parent == null || !String.valueOf(parent.getId()).equals(parentText)) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_REGION_PARENT_INVALID);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        GoldRegionEntity region = new GoldRegionEntity();
        region.setId(IdWorker.getId());
        region.setTenantId(current.tenantId());
        region.setOwnerUserId(current.userId());
        region.setCity(city);
        region.setParentRegionId(parent == null ? null : parent.getId());
        region.setDepth(ancestors.size() + 1);
        region.setStatus("ACTIVE");
        auditNew(region, current.userId(), now);
        try {
            regionMapper.insert(region);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REGION_CONFLICT);
        }
        return toRegion(region);
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional(readOnly = true)
    public GoldRegionVo myRegion() {
        CurrentPrincipal current = requireEnabledCustomer();
        GoldRegionEntity region = regionMapper.selectOne(new LambdaQueryWrapper<GoldRegionEntity>()
                .eq(GoldRegionEntity::getTenantId, current.tenantId())
                .eq(GoldRegionEntity::getOwnerUserId, current.userId())
                .eq(GoldRegionEntity::getDeleted, 0)
                .last("LIMIT 1"));
        return region == null ? null : toRegion(region);
    }

    public boolean available() {
        return properties.enabled();
    }

    /** Called by the WeChat provisioning path after a new customer is created. */
    @Transactional
    public void initializeForCustomer(long tenantId, long userId) {
        if (!available()) return;
        // WeChat provisioning runs before the JWT is created. The tenant interceptor therefore
        // cannot derive a tenant from CurrentUser and would reject this first-login query.
        // Install the known tenant explicitly for the complete read/write operation, then restore
        // any caller context so this method is safe on pooled request threads as well.
        Long previousTenant = TenantContext.get();
        try {
            TenantContext.set(tenantId);
            if (accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                    .eq(GoldBeanAccountEntity::getTenantId, tenantId)
                    .eq(GoldBeanAccountEntity::getUserId, userId)
                    .eq(GoldBeanAccountEntity::getDeleted, 0)
                    .last("LIMIT 1")) != null) return;
            createAccount(tenantId, userId, userId, LocalDateTime.now());
        } finally {
            if (previousTenant == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(previousTenant);
            }
        }
    }

    /** Completes a registration after the payment channel has verified the recipient and payer. */
    @Transactional
    void completeRegistration(
            long tenantId,
            long userId,
            String referralCode,
            Long expectedReferrerId,
            String requestedCity,
            String registrationFeeRecipient,
            String orderNo) {
        if (!available()) throw new BusinessException(ErrorCode.GOLD_BEAN_NOT_ENABLED);
        GoldBeanAccountEntity account = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, tenantId)
                .eq(GoldBeanAccountEntity::getUserId, userId)
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (account == null) throw new BusinessException(ErrorCode.GOLD_BEAN_ACCOUNT_NOT_FOUND);
        settle(account, userId);
        if ("PAID".equals(account.getRegistrationFeeStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_ALREADY_REGISTERED);
        }

        String normalizedReferralCode = referralCode == null
                ? ""
                : referralCode.trim().toUpperCase(Locale.ROOT);
        boolean referredRegistration = "REFERRER".equals(registrationFeeRecipient);
        if (!referredRegistration && !"PLATFORM".equals(registrationFeeRecipient)) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
        }
        GoldBeanAccountEntity referrer = null;
        if (referredRegistration) {
            if (normalizedReferralCode.isBlank() || expectedReferrerId == null
                    || Objects.equals(expectedReferrerId, userId)) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_INVALID);
            }
            referrer = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                    .eq(GoldBeanAccountEntity::getTenantId, tenantId)
                    .eq(GoldBeanAccountEntity::getUserId, expectedReferrerId)
                    .eq(GoldBeanAccountEntity::getReferralCode, normalizedReferralCode)
                    .eq(GoldBeanAccountEntity::getStatus, "ACTIVE")
                    .eq(GoldBeanAccountEntity::getRegistrationFeeStatus, "PAID")
                    .eq(GoldBeanAccountEntity::getDeleted, 0)
                    .last("LIMIT 1 FOR UPDATE"));
            if (referrer == null) throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_INVALID);
        } else if (!normalizedReferralCode.isBlank() || expectedReferrerId != null || hasPlatformRegistration()) {
            // Only the first platform-wide registration may use a platform invite.
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_REGISTRATION_ONLY);
        }

        String city = TextEncodingUtils.repairUtf8Mojibake(
                requestedCity == null ? "" : requestedCity.trim());
        if (!city.isBlank()) {
            if (StringUtils.hasText(account.getCity()) && !city.equals(account.getCity())) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_INVALID);
            }
            account.setCity(city);
        }
        LocalDateTime now = LocalDateTime.now();
        account.setRegistrationFeeStatus("PAID");
        account.setRegistrationFeeRecipient(registrationFeeRecipient);
        account.setRegistrationFeeCent(registrationFeeCent(registrationFeeRecipient));
        account.setRegistrationFeePaidAt(now);
        if (!StringUtils.hasText(account.getReferralCode())) {
            account.setReferralCode(createReferralCode(account.getUserId()));
        }
        if (referrer != null) account.setReferrerId(referrer.getUserId());
        account.setDailyRewardStartAt(now);
        touch(account, userId, now);
        accountMapper.updateById(account);
        // 支付回调确认成功当天只发放第 1 天的每日奖励（60 金豆）。
        accrueDailyRewards(account, userId);
        if (referrer != null) {
            GoldBeanReferralEntity relation = new GoldBeanReferralEntity();
            relation.setId(IdWorker.getId());
            relation.setTenantId(tenantId);
            relation.setReferrerId(referrer.getUserId());
            relation.setReferredId(account.getUserId());
            relation.setReferralCode(normalizedReferralCode);
            relation.setRegistrationFeeCent(properties.registrationFeeCent());
            relation.setRegistrationFeeRecipient(REFERRER);
            relation.setStatus("ACTIVE");
            relation.setRegisteredAt(now);
            auditNew(relation, userId, now);
            try {
                referralMapper.insert(relation);
            } catch (DuplicateKeyException exception) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_ALREADY_REGISTERED);
            }
            processReferralReward(referrer, account, relation, now);
        }
    }

    /** Backward-compatible wrapper for the platform virtual-payment callback. */
    @Transactional
    void completePlatformRegistration(
            long tenantId, long userId, String referralCode, String requestedCity, String orderNo) {
        completeRegistration(tenantId, userId, referralCode, null, requestedCity, PLATFORM, orderNo);
    }

    /** Credits purchased beans into the two ledgers after a verified platform payment. */
    @Transactional
    void completePlatformPurchase(long tenantId, long userId, BigDecimal quantity, String orderNo) {
        if (!available()) throw new BusinessException(ErrorCode.GOLD_BEAN_NOT_ENABLED);
        BigDecimal normalizedQuantity = GoldBeanAmounts.normalize(quantity);
        if (!GoldBeanAmounts.positive(normalizedQuantity)) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        GoldBeanAccountEntity account = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, tenantId)
                .eq(GoldBeanAccountEntity::getUserId, userId)
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (account == null) throw new BusinessException(ErrorCode.GOLD_BEAN_ACCOUNT_NOT_FOUND);
        settle(account, userId);
        requirePlatformPurchaseEligible(account);
        credit(account, normalizedQuantity, "PLATFORM_PURCHASE", null, null,
                "向平台购买金豆（双账本尽量各一半）", "PURCHASE:" + orderNo);
    }

    @Transactional
    void completePlatformPurchase(long tenantId, long userId, long quantity, String orderNo) {
        completePlatformPurchase(tenantId, userId, BigDecimal.valueOf(quantity), orderNo);
    }

    /** Credits a verified legendary-person purchase entirely into the digital bank bucket. */
    @Transactional
    void completeLegendaryBankPurchase(long tenantId, long userId, BigDecimal quantity, String orderNo) {
        if (!available()) throw new BusinessException(ErrorCode.GOLD_BEAN_NOT_ENABLED);
        BigDecimal normalizedQuantity = GoldBeanAmounts.normalize(quantity);
        if (!GoldBeanAmounts.positive(normalizedQuantity)) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        GoldBeanAccountEntity account = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, tenantId)
                .eq(GoldBeanAccountEntity::getUserId, userId)
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (account == null) throw new BusinessException(ErrorCode.GOLD_BEAN_ACCOUNT_NOT_FOUND);
        settle(account, userId);
        if (!"PAID".equals(account.getRegistrationFeeStatus()) || !"ACTIVE".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_REQUIRED);
        }
        creditDigitalBank(account, normalizedQuantity, "LEGENDARY_BANK_PURCHASE", null, null,
                "传奇人物购买金豆（全部进入数字银行）", "LEGENDARY_PURCHASE:" + orderNo);
    }

    @Transactional
    void completeLegendaryBankPurchase(long tenantId, long userId, long quantity, String orderNo) {
        completeLegendaryBankPurchase(tenantId, userId, BigDecimal.valueOf(quantity), orderNo);
    }

    boolean platformPurchaseEligible(GoldBeanAccountEntity account) {
        // Platform direct purchase is a separate entitlement from the legendary market-only
        // path. The payment service additionally rejects currently eligible legendary users.
        return "PAID".equals(account.getRegistrationFeeStatus())
                && (PLATFORM.equals(account.getRegistrationFeeRecipient())
                        || GoldBeanLevel.DIAMOND.code().equalsIgnoreCase(account.getMemberLevel()));
    }

    void requirePlatformPurchaseEligible(GoldBeanAccountEntity account) {
        if (!platformPurchaseEligible(account)) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_PURCHASE_NOT_ELIGIBLE);
        }
    }

    private CurrentPrincipal requireEnabledCustomer() {
        if (!available()) throw new BusinessException(ErrorCode.GOLD_BEAN_NOT_ENABLED);
        CurrentPrincipal current = CurrentUser.require();
        if (!"CUSTOMER".equals(current.workbench())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        return current;
    }

    GoldBeanAccountEntity ensureAccount(CurrentPrincipal current) {
        GoldBeanAccountEntity account = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                .eq(GoldBeanAccountEntity::getUserId, current.userId())
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (account != null) return account;
        try {
            return createAccount(current.tenantId(), current.userId(), current.userId(), LocalDateTime.now());
        } catch (DuplicateKeyException exception) {
            GoldBeanAccountEntity concurrent = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                    .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                    .eq(GoldBeanAccountEntity::getUserId, current.userId())
                    .eq(GoldBeanAccountEntity::getDeleted, 0)
                    .last("LIMIT 1 FOR UPDATE"));
            if (concurrent != null) return concurrent;
            throw exception;
        }
    }

    private GoldBeanAccountEntity createAccount(long tenantId, long userId, long operatorId, LocalDateTime now) {
        GoldBeanAccountEntity account = new GoldBeanAccountEntity();
        account.setId(IdWorker.getId());
        account.setTenantId(tenantId);
        account.setUserId(userId);
        account.setMemberLevel(GoldBeanLevel.ORDINARY.code());
        account.setHistoricalLevel(GoldBeanLevel.ORDINARY.code());
        account.setDirectReferralCount(0);
        account.setReferralCode(null);
        account.setRegistrationFeeStatus("UNPAID");
        account.setRegistrationFeeCent(properties.platformRegistrationFeeCent());
        account.setDigitalBankBalance(GoldBeanAmounts.ZERO);
        account.setTradingBalance(GoldBeanAmounts.ZERO);
        account.setDailyRewardStartAt(now);
        account.setDailyRewardDays(0);
        account.setTradeLimitPercent(100);
        account.setStatus("ACTIVE");
        auditNew(account, operatorId, now);
        accountMapper.insert(account);
        return account;
    }

    private GoldBeanOrderEntity newDevelopmentRegistrationOrder(
            CurrentPrincipal current,
            String orderNo,
            GoldBeanPlatformInviteEntity platformInvite,
            String referralCode,
            GoldBeanAccountEntity referrer,
            String registrationFeeRecipient,
            String city,
            LocalDateTime now) {
        GoldBeanOrderEntity order = new GoldBeanOrderEntity();
        order.setId(IdWorker.getId());
        order.setTenantId(current.tenantId());
        order.setCustomerId(current.userId());
        order.setOrderNo(orderNo);
        order.setOrderType("REGISTRATION_FEE");
        order.setStatus("PAID");
        order.setAmountCent(registrationFeeCent(registrationFeeRecipient));
        order.setGoldBeanQuantity(GoldBeanAmounts.ZERO);
        order.setRegistrationReferralCode(referralCode.isBlank() ? null : referralCode);
        order.setRegistrationReferrerId(referrer == null ? null : referrer.getUserId());
        order.setRegistrationCity(city.isBlank() ? null : city);
        order.setRegistrationFeeRecipient(registrationFeeRecipient);
        order.setSettlementStatus("NOT_REQUIRED");
        if (platformInvite != null) {
            order.setPlatformInviteId(platformInvite.getId());
            order.setPlatformSlotKey(GoldBeanPlatformInviteService.PLATFORM_ROOT_SLOT);
        }
        order.setProductId(StringUtils.hasText(properties.registrationProductId())
                ? properties.registrationProductId()
                : "DEVELOPMENT_RECORD");
        order.setPaymentChannel("DEVELOPMENT_RECORD");
        order.setPaymentNotifyAt(now);
        order.setPaidAt(now);
        order.setCreatedBy(current.userId());
        order.setCreatedAt(now);
        order.setUpdatedBy(current.userId());
        order.setUpdatedAt(now);
        order.setDeleted(0);
        order.setVersion(0);
        return order;
    }

    static String createReferralCode(long userId) {
        String code = ("SY" + Long.toUnsignedString(userId, 36) + UUID.randomUUID().toString().replace("-", "")
                        .substring(0, 8))
                .toUpperCase(Locale.ROOT);
        return code.length() <= 20 ? code : code.substring(0, 20);
    }

    private GoldBeanAccountEntity requiredAccount(CurrentPrincipal current) {
        GoldBeanAccountEntity account = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                .eq(GoldBeanAccountEntity::getUserId, current.userId())
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (account == null) throw new BusinessException(ErrorCode.GOLD_BEAN_ACCOUNT_NOT_FOUND);
        return account;
    }

    private void settle(GoldBeanAccountEntity account, CurrentPrincipal current) {
        settle(account, current.userId());
    }

    private void settle(GoldBeanAccountEntity account, long operatorId) {
        accrueDailyRewards(account, operatorId);
        settleProtection(account, operatorId);
    }

    void settleForTrade(GoldBeanAccountEntity account, long operatorId) {
        settle(account, operatorId);
    }

    /** Refreshes time-based membership state before checking a purchase permission. */
    void settleForPurchase(GoldBeanAccountEntity account, long operatorId) {
        settle(account, operatorId);
    }

    private void accrueDailyRewards(GoldBeanAccountEntity account, long operatorId) {
        if (!"PAID".equals(account.getRegistrationFeeStatus()) || account.getDailyRewardStartAt() == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate next = account.getDailyRewardLastAt() == null
                ? account.getDailyRewardStartAt().toLocalDate()
                : account.getDailyRewardLastAt().toLocalDate().plusDays(1);
        int days = account.getDailyRewardDays() == null ? 0 : account.getDailyRewardDays();
        while (days < properties.dailyRewardDays() && !next.isAfter(today)) {
            credit(account, BigDecimal.valueOf(properties.dailyRewardBeans()), "DAILY_REWARD", null, null,
                    "普通会员每日金豆奖励（第" + (days + 1) + "天）",
                    "DAILY:" + account.getTenantId() + ":" + account.getUserId() + ":" + next);
            days++;
            account.setDailyRewardDays(days);
            account.setDailyRewardLastAt(next.atStartOfDay());
            touch(account, operatorId, LocalDateTime.now());
            accountMapper.updateById(account);
            next = next.plusDays(1);
        }
    }

    private void settleProtection(GoldBeanAccountEntity account, long operatorId) {
        LocalDateTime now = LocalDateTime.now();

        // 普通会员先完成 20 天每日奖励。奖励期内的推荐只记录推荐关系和等级，不能
        // 提前消耗/刷新后续的 7 天活跃保护期。
        if (dailyRewardInProgress(account)) {
            // A user may already be above ordinary membership while still completing the
            // initial 20-day reward. Normalize any stale protection state so unlocking another
            // level during this window cannot start a 7-day period early.
            boolean changed = clearProtectionBeforeDailyRewardCompletion(account);
            if (changed) {
                touch(account, operatorId, now);
                accountMapper.updateById(account);
            }
            return;
        }

        if (!dailyRewardComplete(account)) return;

        // 每日奖励完成后首次结算即开启 7 天保护期。只有已经进入掉级序列的账号不重新开启，
        // 避免保护期结束后访问页面又被自动恢复成新的保护期。
        if (canStartPostRewardProtection(account)) {
            startProtection(account, now, false);
            touch(account, operatorId, now);
            accountMapper.updateById(account);
            return;
        }

        LocalDateTime protectionUntil = account.getProtectionUntil();
        if (protectionUntil != null && !protectionUntil.isBefore(now)) return;

        /*
         * A protection period covers seven days. Its expiration date is therefore the first
         * demotion date (day 8). Keep the expired protectionUntil as the anchor while the
         * account is still above ordinary membership, so every later settlement can apply at
         * most one additional calendar-day demotion.
         */
        LocalDate firstDrop = protectionUntil != null
                ? protectionUntil.toLocalDate()
                : account.getLastLevelDropAt() == null
                        ? null
                        : account.getLastLevelDropAt().toLocalDate().plusDays(1);
        if (firstDrop == null) return;

        GoldBeanLevel current = GoldBeanLevel.fromCode(account.getMemberLevel());
        LocalDate nextDrop = account.getLastLevelDropAt() == null
                ? firstDrop
                : account.getLastLevelDropAt().toLocalDate().plusDays(1);
        if (nextDrop.isBefore(firstDrop)) nextDrop = firstDrop;
        LocalDate today = now.toLocalDate();
        boolean changed = protectionUntil != null
                || account.getProtectionStartedAt() != null
                || account.getLastProtectionReferralAt() != null;
        while (current != GoldBeanLevel.ORDINARY && !nextDrop.isAfter(today)) {
            current = previous(current);
            account.setMemberLevel(current.code());
            account.setLastLevelDropAt(nextDrop.atStartOfDay());
            nextDrop = nextDrop.plusDays(1);
            changed = true;
        }
        if (current == GoldBeanLevel.ORDINARY) {
            // 到达普通会员当天仍保持完整交易额度；只有普通会员继续经历一个自然日的
            // 无推荐状态（即已没有可再掉的会员等级）后，才进入 50% 交易限制。
            int targetTradeLimit = !nextDrop.isAfter(today) ? properties.limitedTradePercent() : 100;
            if (!Objects.equals(account.getTradeLimitPercent(), targetTradeLimit)) {
                account.setTradeLimitPercent(targetTradeLimit);
                changed = true;
            }
            if (account.getProtectionStartedAt() != null) {
                account.setProtectionStartedAt(null);
                changed = true;
            }
            if (account.getProtectionUntil() != null) {
                account.setProtectionUntil(null);
                changed = true;
            }
            if (account.getLastProtectionReferralAt() != null) {
                account.setLastProtectionReferralAt(null);
                changed = true;
            }
        } else {
            // 掉级期间保持完整交易额度；只有到达普通会员后继续经历一个自然日的无推荐状态，
            // 才会在上面的分支进入交易限制。
            if (!Objects.equals(account.getTradeLimitPercent(), 100)) {
                account.setTradeLimitPercent(100);
                changed = true;
            }
            if (account.getProtectionStartedAt() != null) {
                account.setProtectionStartedAt(null);
                changed = true;
            }
            if (account.getLastProtectionReferralAt() != null) {
                account.setLastProtectionReferralAt(null);
                changed = true;
            }
            // Do not clear the expired protectionUntil: it is the first-drop anchor for the
            // next calendar day.
        }
        if (!changed) return;
        touch(account, operatorId, now);
        accountMapper.updateById(account);
    }

    private boolean dailyRewardComplete(GoldBeanAccountEntity account) {
        return "PAID".equals(account.getRegistrationFeeStatus())
                && account.getDailyRewardStartAt() != null
                && (account.getDailyRewardDays() == null
                        ? 0
                        : account.getDailyRewardDays()) >= properties.dailyRewardDays();
    }

    private boolean dailyRewardInProgress(GoldBeanAccountEntity account) {
        return "PAID".equals(account.getRegistrationFeeStatus())
                && account.getDailyRewardStartAt() != null
                && !dailyRewardComplete(account);
    }

    private boolean canStartPostRewardProtection(GoldBeanAccountEntity account) {
        Integer tradeLimitPercent = account.getTradeLimitPercent();
        return account.getProtectionStartedAt() == null
                && account.getProtectionUntil() == null
                && account.getLastProtectionReferralAt() == null
                && account.getLastLevelDropAt() == null
                && (tradeLimitPercent == null || tradeLimitPercent >= 100);
    }

    private boolean clearProtectionBeforeDailyRewardCompletion(GoldBeanAccountEntity account) {
        boolean changed = false;
        if (account.getProtectionStartedAt() != null) {
            account.setProtectionStartedAt(null);
            changed = true;
        }
        if (account.getProtectionUntil() != null) {
            account.setProtectionUntil(null);
            changed = true;
        }
        if (account.getLastProtectionReferralAt() != null) {
            account.setLastProtectionReferralAt(null);
            changed = true;
        }
        if (!Objects.equals(account.getTradeLimitPercent(), 100)) {
            account.setTradeLimitPercent(100);
            changed = true;
        }
        if (account.getLastLevelDropAt() != null) {
            account.setLastLevelDropAt(null);
            changed = true;
        }
        return changed;
    }

    private GoldBeanLevel previous(GoldBeanLevel current) {
        return current == GoldBeanLevel.ORDINARY ? GoldBeanLevel.ORDINARY
                : GoldBeanLevel.values()[current.ordinal() - 1];
    }

    private void processReferralReward(
            GoldBeanAccountEntity referrer,
            GoldBeanAccountEntity referred,
            GoldBeanReferralEntity relation,
            LocalDateTime now) {
        boolean dailyRewardInProgress = dailyRewardInProgress(referrer);
        settle(referrer, referrer.getUserId());
        referrer.setDirectReferralCount(referrer.getDirectReferralCount() + 1);
        updateLevelAfterReferral(referrer, now);
        if (dailyRewardInProgress && !dailyRewardComplete(referrer)) {
            // 第 20 天前的推荐会抑制第 15-19 天提醒，但保护期统一从每日奖励完成后开始。
            clearProtectionBeforeDailyRewardCompletion(referrer);
        } else {
            activateProtection(referrer, now);
        }
        credit(referrer, BigDecimal.valueOf(DIRECT_REFERRAL_REWARD), "REFERRAL_DIRECT", referred.getUserId(), relation.getId(),
                "直推注册奖励", "REFERRAL:DIRECT:" + relation.getId());

        touch(referrer, referrer.getUserId(), now);
        accountMapper.updateById(referrer);

        rewardDownlineReferrers(referrer, referred.getUserId(), relation.getId());
    }

    private void rewardDownlineReferrers(
            GoldBeanAccountEntity directReferrer,
            long referredUserId,
            long relationId) {
        Set<Long> visited = new HashSet<>();
        visited.add(directReferrer.getUserId());
        Long ancestorId = directReferrer.getReferrerId();
        while (ancestorId != null
                && !Objects.equals(ancestorId, referredUserId)
                && visited.add(ancestorId)) {
            GoldBeanAccountEntity ancestor = findAncestorReferrer(directReferrer.getTenantId(), ancestorId);
            if (ancestor == null) return;

            settle(ancestor, ancestor.getUserId());
            credit(ancestor, BigDecimal.valueOf(DOWNLINE_REFERRAL_REWARD), "REFERRAL_DOWNLINE", referredUserId, relationId,
                    "下游推荐注册奖励", "REFERRAL:DOWNLINE:" + relationId + ":" + ancestor.getUserId());
            ancestorId = ancestor.getReferrerId();
        }
    }

    private GoldBeanAccountEntity findAncestorReferrer(long tenantId, long ancestorId) {
        return accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, tenantId)
                .eq(GoldBeanAccountEntity::getUserId, ancestorId)
                .eq(GoldBeanAccountEntity::getStatus, "ACTIVE")
                .eq(GoldBeanAccountEntity::getRegistrationFeeStatus, "PAID")
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
    }

    /**
     * Loads the structural referral chain above an area owner. Three ancestors are loaded so a
     * fourth area level can be rejected instead of silently truncating the hierarchy to two
     * payout recipients.
     */
    List<GoldBeanAccountEntity> loadRegionAncestors(GoldBeanAccountEntity account, boolean lockRows) {
        List<GoldBeanAccountEntity> ancestors = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        visited.add(account.getUserId());
        Long ancestorId = account.getReferrerId();
        while (ancestorId != null && ancestors.size() < 3) {
            if (!visited.add(ancestorId)) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_REGION_DEPTH_EXCEEDED);
            }
            GoldBeanAccountEntity ancestor = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                    .eq(GoldBeanAccountEntity::getTenantId, account.getTenantId())
                    .eq(GoldBeanAccountEntity::getUserId, ancestorId)
                    .eq(GoldBeanAccountEntity::getDeleted, 0)
                    .last(lockRows ? "LIMIT 1 FOR UPDATE" : "LIMIT 1"));
            if (ancestor == null) break;
            ancestors.add(ancestor);
            ancestorId = ancestor.getReferrerId();
        }
        return ancestors;
    }

    private void updateLevelAfterReferral(GoldBeanAccountEntity account, LocalDateTime now) {
        GoldBeanLevel directLevel = GoldBeanLevel.forDirectReferralCount(account.getDirectReferralCount());
        GoldBeanLevel historical = GoldBeanLevel.fromCode(account.getHistoricalLevel());
        GoldBeanLevel current = GoldBeanLevel.fromCode(account.getMemberLevel());
        boolean recoveringFromDemotion = current.ordinal() < historical.ordinal();
        if (directLevel.ordinal() > historical.ordinal()) {
            for (int ordinal = historical.ordinal() + 1; ordinal <= directLevel.ordinal(); ordinal++) {
                GoldBeanLevel unlocked = GoldBeanLevel.values()[ordinal];
                credit(account, BigDecimal.valueOf(unlocked.unlockReward()), "LEVEL_REWARD", null, null,
                        unlocked.displayName() + "解锁奖励", "LEVEL:" + account.getUserId() + ":" + unlocked.code());
            }
            account.setHistoricalLevel(directLevel.code());
            historical = directLevel;
        }

        GoldBeanLevel restored = recoveringFromDemotion
                ? current.next()
                : directLevel.ordinal() > current.ordinal() ? directLevel : current;
        // A recovery can move up only one level and can never exceed the account's historical
        // highest level. At the absolute top, a new referral only refreshes protection.
        if (restored.ordinal() > historical.ordinal()) restored = historical;
        account.setMemberLevel(restored.code());
    }

    private void activateProtection(GoldBeanAccountEntity account, LocalDateTime now) {
        startProtection(account, now, true);
    }

    private void startProtection(GoldBeanAccountEntity account, LocalDateTime now, boolean referral) {
        account.setProtectionStartedAt(now);
        account.setProtectionUntil(now.plusDays(properties.protectionDays()));
        account.setLastProtectionReferralAt(referral ? now : null);
        account.setLastLevelDropAt(null);
        account.setTradeLimitPercent(100);
    }

    private void credit(
            GoldBeanAccountEntity account,
            BigDecimal amount,
            String eventType,
            Long relatedUserId,
            Long relatedReferralId,
            String description,
            String idempotencyPrefix) {
        BigDecimal normalizedAmount = GoldBeanAmounts.normalize(amount);
        if (!GoldBeanAmounts.positive(normalizedAmount)) return;
        BigDecimal bankAmount = GoldBeanAmounts.bankHalf(normalizedAmount);
        BigDecimal tradingAmount = GoldBeanAmounts.tradingHalf(normalizedAmount);
        String bankKey = idempotencyPrefix + ":BANK";
        String tradingKey = idempotencyPrefix + ":TRADING";
        boolean bankExists = ledgerExists(bankKey);
        boolean tradingExists = ledgerExists(tradingKey);
        if (!bankExists) {
            account.setDigitalBankBalance(GoldBeanAmounts.nonNegative(account.getDigitalBankBalance()).add(bankAmount));
            insertLedger(account, "DIGITAL_BANK", bankAmount, eventType, relatedUserId, relatedReferralId,
                    bankKey, description + "（数字银行）");
        }
        if (!tradingExists) {
            account.setTradingBalance(GoldBeanAmounts.nonNegative(account.getTradingBalance()).add(tradingAmount));
            insertLedger(account, "TRADING", tradingAmount, eventType, relatedUserId, relatedReferralId,
                    tradingKey, description + "（可交易）");
        }
        if (!bankExists || !tradingExists) {
            accountMapper.updateById(account);
            if (regionProfitService != null) {
                regionProfitService.distributeForReward(
                        account, normalizedAmount, eventType, relatedUserId, idempotencyPrefix);
            }
        }
    }

    private void creditDigitalBank(
            GoldBeanAccountEntity account,
            BigDecimal amount,
            String eventType,
            Long relatedUserId,
            Long relatedReferralId,
            String description,
            String idempotencyPrefix) {
        BigDecimal normalizedAmount = GoldBeanAmounts.normalize(amount);
        if (!GoldBeanAmounts.positive(normalizedAmount) || ledgerExists(idempotencyPrefix + ":BANK")) return;
        account.setDigitalBankBalance(GoldBeanAmounts.nonNegative(account.getDigitalBankBalance()).add(normalizedAmount));
        insertLedger(account, "DIGITAL_BANK", normalizedAmount, eventType, relatedUserId, relatedReferralId,
                idempotencyPrefix + ":BANK", description + "（数字银行）");
        accountMapper.updateById(account);
    }

    void debitForTrade(
            GoldBeanAccountEntity account,
            String bucket,
            BigDecimal amount,
            String eventType,
            Long relatedUserId,
            String idempotencyKey,
            String description) {
        BigDecimal normalizedAmount = GoldBeanAmounts.normalize(amount);
        if (!GoldBeanAmounts.positive(normalizedAmount) || ledgerExists(idempotencyKey)) return;
        if ("DIGITAL_BANK".equals(bucket)) {
            BigDecimal balance = GoldBeanAmounts.nonNegative(account.getDigitalBankBalance());
            if (balance.compareTo(normalizedAmount) < 0) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_BALANCE_INSUFFICIENT);
            }
            account.setDigitalBankBalance(balance.subtract(normalizedAmount));
        } else if ("TRADING".equals(bucket)) {
            BigDecimal balance = GoldBeanAmounts.nonNegative(account.getTradingBalance());
            if (balance.compareTo(normalizedAmount) < 0) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_BALANCE_INSUFFICIENT);
            }
            account.setTradingBalance(balance.subtract(normalizedAmount));
        } else {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_AMOUNT_INVALID);
        }
        insertLedger(account, bucket, normalizedAmount, eventType, relatedUserId, null,
                idempotencyKey, description, "DEBIT");
        accountMapper.updateById(account);
    }

    /** Debits the digital-bank ledger for a robot entitlement redemption. */
    void debitDigitalBankForRobot(
            GoldBeanAccountEntity account,
            BigDecimal amount,
            long operatorId,
            String idempotencyKey,
            String description) {
        BigDecimal normalizedAmount = GoldBeanAmounts.normalize(amount);
        if (!GoldBeanAmounts.positive(normalizedAmount) || ledgerExists(idempotencyKey)) return;
        BigDecimal balance = GoldBeanAmounts.nonNegative(account.getDigitalBankBalance());
        if (balance.compareTo(normalizedAmount) < 0) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_ROBOT_BALANCE_INSUFFICIENT);
        }
        account.setDigitalBankBalance(balance.subtract(normalizedAmount));
        insertLedger(account, "DIGITAL_BANK", normalizedAmount, "ROBOT_REDEEM", null, null,
                idempotencyKey, description, "DEBIT");
        touch(account, operatorId, LocalDateTime.now());
        accountMapper.updateById(account);
    }

    /** Credits a normal market buyer into both ledgers while preserving six-decimal quantities. */
    void creditForTradeSplit(
            GoldBeanAccountEntity account,
            BigDecimal amount,
            String eventType,
            Long relatedUserId,
            String idempotencyPrefix,
            String description) {
        credit(account, amount, eventType, relatedUserId, null, description, idempotencyPrefix);
    }

    /** Credits a legendary market buyer entirely into the digital-bank ledger. */
    void creditForTradeDigitalBank(
            GoldBeanAccountEntity account,
            BigDecimal amount,
            String eventType,
            Long relatedUserId,
            String idempotencyPrefix,
            String description) {
        creditDigitalBank(account, amount, eventType, relatedUserId, null, description, idempotencyPrefix);
    }

    /** Credits an automatically calculated regional reward through the same two-ledger split. */
    void creditRegionProfit(
            GoldBeanAccountEntity account,
            BigDecimal amount,
            Long relatedUserId,
            String idempotencyPrefix,
            String description) {
        credit(account, amount, "REGION_REWARD", relatedUserId, null, description, idempotencyPrefix);
    }

    private boolean ledgerExists(String key) {
        return ledgerMapper.selectOne(new LambdaQueryWrapper<GoldBeanLedgerEntity>()
                .eq(GoldBeanLedgerEntity::getIdempotencyKey, key)
                .eq(GoldBeanLedgerEntity::getDeleted, 0)
                .last("LIMIT 1")) != null;
    }

    private void insertLedger(
            GoldBeanAccountEntity account,
            String bucket,
            BigDecimal amount,
            String eventType,
            Long relatedUserId,
            Long relatedReferralId,
            String idempotencyKey,
            String description) {
        insertLedger(account, bucket, amount, eventType, relatedUserId, relatedReferralId,
                idempotencyKey, description, "CREDIT");
    }

    private void insertLedger(
            GoldBeanAccountEntity account,
            String bucket,
            BigDecimal amount,
            String eventType,
            Long relatedUserId,
            Long relatedReferralId,
            String idempotencyKey,
            String description,
            String direction) {
        LocalDateTime now = LocalDateTime.now();
        GoldBeanLedgerEntity ledger = new GoldBeanLedgerEntity();
        ledger.setId(IdWorker.getId());
        ledger.setTenantId(account.getTenantId());
        ledger.setUserId(account.getUserId());
        ledger.setBucket(bucket);
        ledger.setDirection(direction);
        ledger.setAmount(GoldBeanAmounts.normalize(amount));
        ledger.setEventType(eventType);
        ledger.setRelatedUserId(relatedUserId);
        ledger.setRelatedReferralId(relatedReferralId);
        ledger.setIdempotencyKey(idempotencyKey);
        ledger.setDescription(description);
        auditNew(ledger, account.getUserId(), now);
        try {
            ledgerMapper.insert(ledger);
        } catch (DuplicateKeyException ignored) {
            // A concurrent retry already committed this idempotent entry.
        }
    }

    private GoldBeanSummaryVo toSummary(GoldBeanAccountEntity account, CurrentPrincipal current) {
        GoldBeanLevel level = GoldBeanLevel.fromCode(account.getMemberLevel());
        GoldBeanLevel historical = GoldBeanLevel.fromCode(account.getHistoricalLevel());
        GoldBeanLevel next = level.next();
        String nextLevel = next == level ? null : next.code();
        String nextLevelName = next == level ? null : next.displayName();
        int nextThreshold = next == level ? level.referralThreshold() : next.referralThreshold();
        String reminder = reminder(account);
        String referralCode = "PAID".equals(account.getRegistrationFeeStatus())
                ? account.getReferralCode()
                : null;
        GoldRegionEntity region = regionMapper.selectOne(new LambdaQueryWrapper<GoldRegionEntity>()
                .eq(GoldRegionEntity::getTenantId, current.tenantId())
                .eq(GoldRegionEntity::getOwnerUserId, current.userId())
                .eq(GoldRegionEntity::getDeleted, 0)
                .last("LIMIT 1"));
        int dailyDays = account.getDailyRewardDays() == null ? 0 : account.getDailyRewardDays();
        return new GoldBeanSummaryVo(
                true,
                String.valueOf(account.getUserId()),
                referralCode,
                level.code(),
                level.displayName(),
                historical.code(),
                historical.displayName(),
                account.getDirectReferralCount(),
                nextLevel,
                nextLevelName,
                nextThreshold,
                account.getRegistrationFeeStatus(),
                account.getRegistrationFeeRecipient(),
                GoldBeanAmounts.nonNegative(account.getDigitalBankBalance())
                        .add(GoldBeanAmounts.nonNegative(account.getTradingBalance())),
                GoldBeanAmounts.nonNegative(account.getDigitalBankBalance()),
                GoldBeanAmounts.nonNegative(account.getTradingBalance()),
                account.getTradeLimitPercent(),
                dailyDays,
                properties.dailyRewardDays(),
                Math.max(0, properties.dailyRewardDays() - dailyDays),
                reminder,
                account.getProtectionUntil(),
                level == GoldBeanLevel.DIAMOND && region == null,
                region == null ? null : String.valueOf(region.getId()),
                region == null ? null : TextEncodingUtils.repairUtf8Mojibake(region.getCity()),
                properties.paymentEnabled(),
                account.getRegistrationFeeCent() == null
                        ? properties.platformRegistrationFeeCent()
                        : account.getRegistrationFeeCent(),
                platformPurchaseEligible(account),
                properties.goldBeanUnitPriceCent(),
                properties.maxPurchaseQuantity(),
                properties.platformRegistrationFeeCent(),
                properties.registrationFeeCent(),
                properties.virtualPaymentSurchargePercent());
    }

    private int registrationFeeCent(String recipient) {
        return REFERRER.equals(recipient)
                ? properties.registrationFeeCent()
                : properties.platformRegistrationFeeCent();
    }

    private String reminder(GoldBeanAccountEntity account) {
        LocalDateTime now = LocalDateTime.now();
        int days = account.getDailyRewardDays() == null ? 0 : account.getDailyRewardDays();
        GoldBeanLevel level = GoldBeanLevel.fromCode(account.getMemberLevel());
        if (account.getProtectionStartedAt() != null && account.getProtectionUntil() != null
                && account.getProtectionUntil().isAfter(now)) {
            long activeDay = ChronoUnit.DAYS.between(
                    account.getProtectionStartedAt().toLocalDate(), now.toLocalDate()) + 1;
            int lastReminderDay = Math.min(6, properties.protectionDays() - 1);
            if (activeDay >= 4 && activeDay <= lastReminderDay) {
                return level.displayName() + "活跃保护期第" + activeDay
                        + "天，请在" + properties.protectionDays()
                        + "天内推荐1位新人；成功推荐后将刷新"
                        + properties.protectionDays() + "天保护期。";
            }
        }
        if (account.getProtectionUntil() != null && account.getProtectionUntil().isBefore(now)
                && level != GoldBeanLevel.ORDINARY) {
            return "当前等级活跃保护期已结束，掉级期间保持100%交易额度；未推荐新人掉至普通会员后次日限制50%，完成直推可刷新"
                    + properties.protectionDays() + "天保护期。";
        }
        if (account.getProtectionUntil() == null
                && account.getLastLevelDropAt() != null
                && level == GoldBeanLevel.ORDINARY
                && account.getTradeLimitPercent() >= 100) {
            return "当前已降至普通会员，继续未推荐新人将限制50%交易额度；完成直推可恢复1个等级并刷新"
                    + properties.protectionDays() + "天保护期。";
        }
        if (days >= 15 && days < properties.dailyRewardDays() && account.getDirectReferralCount() == 0) {
            return "你已连续" + (days - 14) + "天没有推荐新人，请在第20天前完成一次直推。";
        }
        if (account.getProtectionUntil() == null && account.getLastLevelDropAt() != null
                && level != GoldBeanLevel.ORDINARY) {
            return "当前处于掉等级状态，完成一次直推可恢复1个等级并重新进入7天活跃保护期。";
        }
        if (account.getTradeLimitPercent() < 100) {
            return "当前处于交易额度限制状态，完成一次直推后可恢复100%交易额度。";
        }
        return null;
    }

    private GoldRegionVo toRegion(GoldRegionEntity region) {
        return new GoldRegionVo(
                String.valueOf(region.getId()),
                TextEncodingUtils.repairUtf8Mojibake(region.getCity()),
                region.getDepth(),
                region.getParentRegionId() == null ? null : String.valueOf(region.getParentRegionId()),
                region.getStatus(),
                region.getCreatedAt());
    }

    private void auditNew(Object entity, long userId, LocalDateTime now) {
        if (entity instanceof GoldBeanAccountEntity value) {
            value.setCreatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedBy(userId);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        } else if (entity instanceof GoldBeanReferralEntity value) {
            value.setCreatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedBy(userId);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        } else if (entity instanceof GoldBeanLedgerEntity value) {
            value.setCreatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedBy(userId);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        } else if (entity instanceof GoldRegionEntity value) {
            value.setCreatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedBy(userId);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        }
    }

    private void touch(GoldBeanAccountEntity account, long userId, LocalDateTime now) {
        account.setUpdatedBy(userId);
        account.setUpdatedAt(now);
    }

    private void touch(Object entity, long userId, LocalDateTime now) {
        if (entity instanceof GoldBeanAccountEntity account) touch(account, userId, now);
    }
}
