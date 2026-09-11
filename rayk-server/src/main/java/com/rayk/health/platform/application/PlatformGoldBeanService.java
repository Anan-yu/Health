package com.rayk.health.platform.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.patient.entity.PatientEntity;
import com.rayk.health.patient.mapper.PatientMapper;
import com.rayk.health.goldbean.application.GoldBeanLevel;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanLedgerEntity;
import com.rayk.health.goldbean.entity.GoldBeanOrderEntity;
import com.rayk.health.goldbean.entity.GoldBeanReferralEntity;
import com.rayk.health.goldbean.entity.GoldRegionEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanLedgerMapper;
import com.rayk.health.goldbean.mapper.GoldBeanOrderMapper;
import com.rayk.health.goldbean.mapper.GoldBeanReferralMapper;
import com.rayk.health.goldbean.mapper.GoldRegionMapper;
import com.rayk.health.goldbean.util.GoldBeanAmounts;
import com.rayk.health.platform.vo.PlatformGoldBeanAccountVo;
import com.rayk.health.platform.vo.PlatformGoldBeanLedgerVo;
import com.rayk.health.platform.vo.PlatformGoldBeanOverviewVo;
import com.rayk.health.platform.vo.PlatformGoldBeanOrderVo;
import com.rayk.health.platform.vo.PlatformGoldBeanReferralVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.wechat.PhoneIdentity;
import com.rayk.health.system.entity.SysTenantEntity;
import com.rayk.health.system.entity.SysUserEntity;
import com.rayk.health.system.mapper.SysTenantMapper;
import com.rayk.health.system.mapper.SysUserMapper;
import com.rayk.health.tenant.TenantContext;
import java.util.List;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Read-only cross-tenant operations for the gold-bean admin console. */
@Service
public class PlatformGoldBeanService {
    private final GoldBeanProperties properties;
    private final GoldBeanAccountMapper accountMapper;
    private final GoldBeanReferralMapper referralMapper;
    private final GoldBeanLedgerMapper ledgerMapper;
    private final GoldBeanOrderMapper orderMapper;
    private final GoldRegionMapper regionMapper;
    private final PatientMapper patientMapper;
    private final SysUserMapper userMapper;
    private final SysTenantMapper tenantMapper;

    public PlatformGoldBeanService(
            GoldBeanProperties properties,
            GoldBeanAccountMapper accountMapper,
            GoldBeanReferralMapper referralMapper,
            GoldBeanLedgerMapper ledgerMapper,
            GoldBeanOrderMapper orderMapper,
            GoldRegionMapper regionMapper,
            PatientMapper patientMapper,
            SysUserMapper userMapper,
            SysTenantMapper tenantMapper) {
        this.properties = properties;
        this.accountMapper = accountMapper;
        this.referralMapper = referralMapper;
        this.ledgerMapper = ledgerMapper;
        this.orderMapper = orderMapper;
        this.regionMapper = regionMapper;
        this.patientMapper = patientMapper;
        this.userMapper = userMapper;
        this.tenantMapper = tenantMapper;
    }

    @Transactional(readOnly = true)
    public PlatformGoldBeanOverviewVo overview() {
        requireAvailablePlatformAccess();
        Snapshot snapshot = loadSnapshot();
        List<PlatformGoldBeanAccountVo> accounts = accountVos(snapshot);
        long registered = accounts.stream().filter(item -> "PAID".equals(item.registrationFeeStatus())).count();
        long platformFees = accounts.stream().filter(item -> "PLATFORM".equals(item.feeRecipientType())).count();
        long referrerFees = accounts.stream().filter(item -> "REFERRER".equals(item.feeRecipientType())).count();
        BigDecimal totalBalance = accounts.stream().map(PlatformGoldBeanAccountVo::totalBalance)
                .reduce(GoldBeanAmounts.ZERO, BigDecimal::add);
        BigDecimal bankBalance = accounts.stream().map(PlatformGoldBeanAccountVo::digitalBankBalance)
                .reduce(GoldBeanAmounts.ZERO, BigDecimal::add);
        BigDecimal tradingBalance = accounts.stream().map(PlatformGoldBeanAccountVo::tradingBalance)
                .reduce(GoldBeanAmounts.ZERO, BigDecimal::add);
        BigDecimal dailyRewardBeans = snapshot.ledgers().stream()
                .filter(item -> "DAILY_REWARD".equals(item.getEventType()) && "CREDIT".equals(item.getDirection()))
                .map(item -> safeAmount(item.getAmount()))
                .reduce(GoldBeanAmounts.ZERO, BigDecimal::add);
        List<PlatformGoldBeanLedgerVo> recent = snapshot.ledgers().stream()
                .limit(8)
                .map(item -> toLedger(item, snapshot.users(), snapshot.patientNames(), snapshot.tenants()))
                .toList();
        return new PlatformGoldBeanOverviewVo(
                properties.enabled(),
                properties.developmentMode(),
                !properties.paymentEnabled(),
                centsToYuan(properties.platformRegistrationFeeCent()),
                properties.initialBeans(),
                properties.dailyRewardBeans(),
                properties.dailyRewardDays(),
                properties.protectionDays(),
                properties.limitedTradePercent(),
                "PLATFORM_GLOBAL",
                accounts.size(),
                registered,
                accounts.size() - registered,
                platformFees,
                referrerFees,
                totalBalance,
                bankBalance,
                tradingBalance,
                dailyRewardBeans,
                snapshot.regions().stream().filter(item -> "ACTIVE".equals(item.getStatus())).count(),
                recent);
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformGoldBeanAccountVo> accounts(String keyword, String registrationStatus) {
        requireAvailablePlatformAccess();
        Snapshot snapshot = loadSnapshot();
        List<PlatformGoldBeanAccountVo> all = accountVos(snapshot);
        String normalizedKeyword = normalize(keyword);
        String normalizedStatus = normalize(registrationStatus);
        List<PlatformGoldBeanAccountVo> records = all.stream()
                .filter(item -> normalizedStatus.isBlank()
                        || "all".equals(normalizedStatus)
                        || normalizedStatus.equals(normalize(item.registrationFeeStatus())))
                .filter(item -> normalizedKeyword.isBlank()
                        || accountMatches(item, normalizedKeyword, snapshot.users()))
                .limit(200)
                .toList();
        return new PageResponse<>(records, records.size(), 1, Math.max(records.size(), 1));
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformGoldBeanReferralVo> referrals(String keyword) {
        requireAvailablePlatformAccess();
        Snapshot snapshot = loadSnapshot();
        String normalizedKeyword = normalize(keyword);
        List<PlatformGoldBeanReferralVo> records = snapshot.referrals().stream()
                .map(item -> toReferral(item, snapshot.users(), snapshot.patientNames(), snapshot.tenants()))
                .filter(item -> normalizedKeyword.isBlank()
                        || referralMatches(item, normalizedKeyword, snapshot.users()))
                .limit(200)
                .toList();
        return new PageResponse<>(records, records.size(), 1, Math.max(records.size(), 1));
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformGoldBeanLedgerVo> ledger(String keyword, String eventType) {
        requireAvailablePlatformAccess();
        Snapshot snapshot = loadSnapshot();
        String normalizedKeyword = normalize(keyword);
        String normalizedEventType = normalize(eventType);
        List<PlatformGoldBeanLedgerVo> records = snapshot.ledgers().stream()
                .map(item -> toLedger(item, snapshot.users(), snapshot.patientNames(), snapshot.tenants()))
                .filter(item -> normalizedEventType.isBlank()
                        || "all".equals(normalizedEventType)
                        || normalizedEventType.equals(normalize(item.eventType())))
                .filter(item -> normalizedKeyword.isBlank()
                        || ledgerMatches(item, normalizedKeyword, snapshot.users()))
                .limit(200)
                .toList();
        return new PageResponse<>(records, records.size(), 1, Math.max(records.size(), 1));
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformGoldBeanOrderVo> orders(String keyword, String status, String orderType) {
        requireAvailablePlatformAccess();
        Snapshot snapshot = loadSnapshot();
        String normalizedKeyword = normalize(keyword);
        String normalizedStatus = normalize(status);
        String normalizedOrderType = normalize(orderType);
        List<PlatformGoldBeanOrderVo> records = snapshot.orders().stream()
                .map(item -> toOrder(item, snapshot.users(), snapshot.patientNames(), snapshot.tenants()))
                .filter(item -> normalizedStatus.isBlank()
                        || "all".equals(normalizedStatus)
                        || normalizedStatus.equals(normalize(item.status())))
                .filter(item -> normalizedOrderType.isBlank()
                        || "all".equals(normalizedOrderType)
                        || normalizedOrderType.equals(normalize(item.orderType())))
                .filter(item -> normalizedKeyword.isBlank()
                        || orderMatches(item, normalizedKeyword, snapshot.users()))
                .limit(200)
                .toList();
        return new PageResponse<>(records, records.size(), 1, Math.max(records.size(), 1));
    }

    private Snapshot loadSnapshot() {
        return TenantContext.executeReadWithoutTenant(() -> {
            List<GoldBeanAccountEntity> accounts = accountMapper.selectList(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                    .eq(GoldBeanAccountEntity::getDeleted, 0)
                    .ne(GoldBeanAccountEntity::getTenantId, 1L)
                    .orderByDesc(GoldBeanAccountEntity::getCreatedAt));
            List<GoldBeanReferralEntity> referrals = referralMapper.selectList(
                    new LambdaQueryWrapper<GoldBeanReferralEntity>()
                            .eq(GoldBeanReferralEntity::getDeleted, 0)
                            .ne(GoldBeanReferralEntity::getTenantId, 1L)
                            .orderByDesc(GoldBeanReferralEntity::getRegisteredAt));
            List<GoldBeanLedgerEntity> ledgers = ledgerMapper.selectList(
                    new LambdaQueryWrapper<GoldBeanLedgerEntity>()
                            .eq(GoldBeanLedgerEntity::getDeleted, 0)
                            .ne(GoldBeanLedgerEntity::getTenantId, 1L)
                            .orderByDesc(GoldBeanLedgerEntity::getCreatedAt));
            List<GoldBeanOrderEntity> orders = orderMapper.selectList(
                    new LambdaQueryWrapper<GoldBeanOrderEntity>()
                            .eq(GoldBeanOrderEntity::getDeleted, 0)
                            .ne(GoldBeanOrderEntity::getTenantId, 1L)
                            .orderByDesc(GoldBeanOrderEntity::getCreatedAt));
            List<GoldRegionEntity> regions = regionMapper.selectList(new LambdaQueryWrapper<GoldRegionEntity>()
                    .eq(GoldRegionEntity::getDeleted, 0)
                    .ne(GoldRegionEntity::getTenantId, 1L));
            Map<Long, SysUserEntity> users = userMapper.selectAllIgnoringTenant().stream()
                    .collect(Collectors.toMap(SysUserEntity::getId, Function.identity(), (left, right) -> left));
            Map<Long, SysTenantEntity> tenants = tenantMapper.selectList(new LambdaQueryWrapper<SysTenantEntity>()
                            .eq(SysTenantEntity::getDeleted, 0)
                            .ne(SysTenantEntity::getTenantId, 1L))
                    .stream()
                    .collect(Collectors.toMap(SysTenantEntity::getTenantId, Function.identity(), (left, right) -> left));
            Map<Long, String> patientNames = patientMapper.selectList(new LambdaQueryWrapper<PatientEntity>()
                            .eq(PatientEntity::getDeleted, 0)
                            .ne(PatientEntity::getTenantId, 1L)
                            .isNotNull(PatientEntity::getUserId)
                            .orderByDesc(PatientEntity::getUpdatedAt)
                            .orderByDesc(PatientEntity::getCreatedAt))
                    .stream()
                    .filter(item -> item.getUserId() != null && StringUtils.hasText(item.getName()))
                    .collect(Collectors.toMap(
                            PatientEntity::getUserId,
                            item -> item.getName().trim(),
                            (left, right) -> left));
            return new Snapshot(accounts, referrals, ledgers, orders, regions, users, tenants, patientNames);
        });
    }

    private List<PlatformGoldBeanAccountVo> accountVos(Snapshot snapshot) {
        return snapshot.accounts().stream()
                .map(item -> toAccount(item, snapshot.users(), snapshot.patientNames(), snapshot.tenants()))
                .toList();
    }

    private PlatformGoldBeanAccountVo toAccount(
            GoldBeanAccountEntity account,
            Map<Long, SysUserEntity> users,
            Map<Long, String> patientNames,
            Map<Long, SysTenantEntity> tenants) {
        GoldBeanLevel level = GoldBeanLevel.fromCode(account.getMemberLevel());
        GoldBeanLevel historical = GoldBeanLevel.fromCode(account.getHistoricalLevel());
        int dailyDays = safeInt(account.getDailyRewardDays());
        int totalDailyDays = properties.dailyRewardDays();
        String referrerName = userName(users.get(account.getReferrerId()), patientNames);
        return new PlatformGoldBeanAccountVo(
                id(account.getId()),
                id(account.getTenantId()),
                tenantName(tenants.get(account.getTenantId())),
                id(account.getUserId()),
                userName(users.get(account.getUserId()), patientNames),
                phone(users.get(account.getUserId())),
                level.code(),
                level.displayName(),
                historical.code(),
                historical.displayName(),
                safeInt(account.getDirectReferralCount()),
                id(account.getReferrerId()),
                referrerName,
                account.getReferralCode(),
                account.getRegistrationFeeStatus(),
                account.getRegistrationFeeRecipient(),
                feeRecipientName(account, referrerName),
                centsToYuan(account.getRegistrationFeeCent()),
                account.getRegistrationFeePaidAt(),
                safeAmount(account.getDigitalBankBalance()).add(safeAmount(account.getTradingBalance())),
                safeAmount(account.getDigitalBankBalance()),
                safeAmount(account.getTradingBalance()),
                safeInt(account.getTradeLimitPercent()),
                dailyDays,
                totalDailyDays,
                Math.max(0, totalDailyDays - dailyDays),
                account.getProtectionUntil(),
                account.getCity(),
                account.getStatus(),
                account.getCreatedAt());
    }

    private PlatformGoldBeanReferralVo toReferral(
            GoldBeanReferralEntity referral,
            Map<Long, SysUserEntity> users,
            Map<Long, String> patientNames,
            Map<Long, SysTenantEntity> tenants) {
        String referrerName = userName(users.get(referral.getReferrerId()), patientNames);
        return new PlatformGoldBeanReferralVo(
                id(referral.getId()),
                id(referral.getTenantId()),
                tenantName(tenants.get(referral.getTenantId())),
                id(referral.getReferrerId()),
                referrerName,
                id(referral.getReferredId()),
                userName(users.get(referral.getReferredId()), patientNames),
                referral.getReferralCode(),
                centsToYuan(referral.getRegistrationFeeCent()),
                referral.getRegistrationFeeRecipient(),
                "REFERRER".equals(referral.getRegistrationFeeRecipient()) ? referrerName : "平台",
                referral.getStatus(),
                referral.getRegisteredAt());
    }

    private PlatformGoldBeanLedgerVo toLedger(
            GoldBeanLedgerEntity ledger,
            Map<Long, SysUserEntity> users,
            Map<Long, String> patientNames,
            Map<Long, SysTenantEntity> tenants) {
        return new PlatformGoldBeanLedgerVo(
                id(ledger.getId()),
                id(ledger.getTenantId()),
                tenantName(tenants.get(ledger.getTenantId())),
                id(ledger.getUserId()),
                userName(users.get(ledger.getUserId()), patientNames),
                ledger.getBucket(),
                ledger.getDirection(),
                safeAmount(ledger.getAmount()),
                ledger.getEventType(),
                ledger.getDescription(),
                ledger.getCreatedAt());
    }

    private PlatformGoldBeanOrderVo toOrder(
            GoldBeanOrderEntity order,
            Map<Long, SysUserEntity> users,
            Map<Long, String> patientNames,
            Map<Long, SysTenantEntity> tenants) {
        return new PlatformGoldBeanOrderVo(
                order.getOrderNo(),
                id(order.getTenantId()),
                tenantName(tenants.get(order.getTenantId())),
                id(order.getCustomerId()),
                userName(users.get(order.getCustomerId()), patientNames),
                order.getOrderType(),
                order.getStatus(),
                safeInt(order.getAmountCent()),
                safeInt(order.getPaymentAmountCent() == null ? order.getAmountCent() : order.getPaymentAmountCent()),
                safeAmount(order.getGoldBeanQuantity()),
                "PLATFORM".equals(order.getRegistrationFeeRecipient())
                        ? "平台"
                        : userName(users.get(order.getRegistrationReferrerId()), patientNames),
                order.getPaymentChannel(),
                maskTransactionId(order.getTransactionId()),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getSettlementStatus(),
                order.getSettlementFailureReason());
    }

    private boolean accountMatches(
            PlatformGoldBeanAccountVo item,
            String keyword,
            Map<Long, SysUserEntity> users) {
        return contains(item.displayName(), keyword)
                || contains(item.phoneMasked(), keyword)
                || userMatches(users, item.userId(), keyword);
    }

    private boolean referralMatches(
            PlatformGoldBeanReferralVo item,
            String keyword,
            Map<Long, SysUserEntity> users) {
        return contains(item.referrerName(), keyword)
                || contains(item.referredName(), keyword)
                || userMatches(users, item.referrerId(), keyword)
                || userMatches(users, item.referredId(), keyword);
    }

    private boolean ledgerMatches(
            PlatformGoldBeanLedgerVo item,
            String keyword,
            Map<Long, SysUserEntity> users) {
        return contains(item.displayName(), keyword)
                || userMatches(users, item.userId(), keyword);
    }

    private boolean orderMatches(
            PlatformGoldBeanOrderVo item,
            String keyword,
            Map<Long, SysUserEntity> users) {
        return contains(item.customerName(), keyword)
                || userMatches(users, item.customerId(), keyword);
    }

    private static boolean userMatches(
            Map<Long, SysUserEntity> users,
            String userId,
            String keyword) {
        Long parsedUserId = parseId(userId);
        if (parsedUserId == null) return false;
        SysUserEntity user = users.get(parsedUserId);
        return user != null
                && (contains(user.getDisplayName(), keyword) || phoneMatches(user, keyword));
    }

    private static boolean phoneMatches(SysUserEntity user, String keyword) {
        if (contains(user.getPhoneMasked(), keyword)) return true;
        if (!StringUtils.hasText(user.getPhoneHash())) return false;
        try {
            return PhoneIdentity.hash(keyword).equalsIgnoreCase(user.getPhoneHash());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void requireAvailablePlatformAccess() {
        CurrentPrincipal current = CurrentUser.require();
        if (!current.roles().contains("PLATFORM_ADMIN")) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        if (!properties.enabled()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_NOT_ENABLED);
        }
    }

    private static boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Long parseId(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String userName(SysUserEntity user) {
        return user == null || !StringUtils.hasText(user.getDisplayName()) ? "未命名用户" : user.getDisplayName();
    }

    private static String userName(SysUserEntity user, Map<Long, String> patientNames) {
        String patientName = user == null || patientNames == null ? null : patientNames.get(user.getId());
        return StringUtils.hasText(patientName) ? patientName : userName(user);
    }

    private static String phone(SysUserEntity user) {
        return user == null || !StringUtils.hasText(user.getPhoneMasked()) ? "手机号已隐藏" : user.getPhoneMasked();
    }

    private static String tenantName(SysTenantEntity tenant) {
        return tenant == null || !StringUtils.hasText(tenant.getTenantName()) ? "未命名机构" : tenant.getTenantName();
    }

    private static String feeRecipientName(GoldBeanAccountEntity account, String referrerName) {
        if ("PLATFORM".equals(account.getRegistrationFeeRecipient())) return "平台";
        if ("REFERRER".equals(account.getRegistrationFeeRecipient())) return referrerName;
        return "未分配";
    }

    private static int centsToYuan(Integer cents) {
        return Math.max(0, cents == null ? 0 : cents) / 100;
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal safeAmount(BigDecimal value) {
        return GoldBeanAmounts.nonNegative(value);
    }

    private static String maskTransactionId(String value) {
        if (!StringUtils.hasText(value)) return "—";
        String normalized = value.trim();
        return normalized.length() <= 8 ? normalized : "••••" + normalized.substring(normalized.length() - 8);
    }

    private record Snapshot(
            List<GoldBeanAccountEntity> accounts,
            List<GoldBeanReferralEntity> referrals,
            List<GoldBeanLedgerEntity> ledgers,
            List<GoldBeanOrderEntity> orders,
            List<GoldRegionEntity> regions,
            Map<Long, SysUserEntity> users,
            Map<Long, SysTenantEntity> tenants,
            Map<Long, String> patientNames) {}
}
