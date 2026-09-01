package com.rayk.health.goldbean.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.dto.OpenGoldRegionRequest;
import com.rayk.health.goldbean.dto.RegisterGoldBeanRequest;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanLedgerEntity;
import com.rayk.health.goldbean.entity.GoldBeanReferralEntity;
import com.rayk.health.goldbean.entity.GoldRegionEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanLedgerMapper;
import com.rayk.health.goldbean.mapper.GoldBeanReferralMapper;
import com.rayk.health.goldbean.mapper.GoldRegionMapper;
import com.rayk.health.goldbean.vo.GoldBeanLedgerVo;
import com.rayk.health.goldbean.vo.GoldBeanSummaryVo;
import com.rayk.health.goldbean.vo.GoldRegionVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Development-only implementation of the new gold-bean membership rules.
 *
 * <p>All balances are derived from an append-only ledger and the account row is updated in the
 * same transaction. This service intentionally has no withdrawal or cash-out operation.
 */
@Service
public class GoldBeanApplicationService {
    private static final int DIRECT_REFERRAL_REWARD = 11;
    private static final int SECOND_LEVEL_REFERRAL_REWARD = 5;
    private static final String PLATFORM = "PLATFORM";
    private static final String REFERRER = "REFERRER";

    private final GoldBeanProperties properties;
    private final GoldBeanAccountMapper accountMapper;
    private final GoldBeanReferralMapper referralMapper;
    private final GoldBeanLedgerMapper ledgerMapper;
    private final GoldRegionMapper regionMapper;

    public GoldBeanApplicationService(
            GoldBeanProperties properties,
            GoldBeanAccountMapper accountMapper,
            GoldBeanReferralMapper referralMapper,
            GoldBeanLedgerMapper ledgerMapper,
            GoldRegionMapper regionMapper) {
        this.properties = properties;
        this.accountMapper = accountMapper;
        this.referralMapper = referralMapper;
        this.ledgerMapper = ledgerMapper;
        this.regionMapper = regionMapper;
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
     * Completes the 998-yuan registration record in the dev pilot. The endpoint records whether
     * the fee belongs to the platform or the direct referrer; it does not create a fake payment.
     */
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public GoldBeanSummaryVo register(RegisterGoldBeanRequest request) {
        CurrentPrincipal current = requireEnabledCustomer();
        GoldBeanAccountEntity account = ensureAccount(current);
        settle(account, current);
        if ("PAID".equals(account.getRegistrationFeeStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_ALREADY_REGISTERED);
        }

        String referralCode = request == null || request.referralCode() == null
                ? ""
                : request.referralCode().trim().toUpperCase(Locale.ROOT);
        GoldBeanAccountEntity referrer = null;
        String recipient = PLATFORM;
        if (!referralCode.isBlank()) {
            referrer = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                    .eq(GoldBeanAccountEntity::getReferralCode, referralCode)
                    .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                    .eq(GoldBeanAccountEntity::getStatus, "ACTIVE")
                    .eq(GoldBeanAccountEntity::getDeleted, 0)
                    .last("LIMIT 1 FOR UPDATE"));
            if (referrer == null || Objects.equals(referrer.getUserId(), account.getUserId())) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_INVALID);
            }
            recipient = REFERRER;
        } else if (accountMapper.selectCount(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                .eq(GoldBeanAccountEntity::getRegistrationFeeStatus, "PAID")
                .eq(GoldBeanAccountEntity::getDeleted, 0)) > 0) {
            // Only the first platform registration may omit a direct referrer.
            throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_INVALID);
        }

        String requestedCity = request == null || request.city() == null ? "" : request.city().trim();
        if (!requestedCity.isBlank()) {
            if (StringUtils.hasText(account.getCity()) && !requestedCity.equals(account.getCity())) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_INVALID);
            }
            account.setCity(requestedCity);
        }
        LocalDateTime now = LocalDateTime.now();
        account.setRegistrationFeeStatus("PAID");
        account.setRegistrationFeeRecipient(recipient);
        account.setRegistrationFeeCent(properties.registrationFeeCent());
        account.setRegistrationFeePaidAt(now);
        if (referrer != null) {
            account.setReferrerId(referrer.getUserId());
        }
        // Customer accounts are provisioned before membership registration so the client can
        // display onboarding state. Initial and daily beans start only after registration.
        account.setDailyRewardStartAt(now);
        touch(account, current.userId(), now);
        accountMapper.updateById(account);
        credit(account, properties.initialBeans(), "INITIAL_GRANT", null, null,
                "普通会员初始金豆（数字银行与可交易各一半）",
                "INITIAL:" + current.tenantId() + ":" + current.userId());

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
            processReferralReward(referrer, account, relation, current.tenantId(), now);
        }
        return toSummary(account, current);
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
        if (regionMapper.selectOne(new LambdaQueryWrapper<GoldRegionEntity>()
                .eq(GoldRegionEntity::getTenantId, current.tenantId())
                .eq(GoldRegionEntity::getOwnerUserId, current.userId())
                .eq(GoldRegionEntity::getDeleted, 0)
                .last("LIMIT 1")) != null) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REGION_CONFLICT);
        }
        String city = request == null || request.city() == null ? "" : request.city().trim();
        if (city.isBlank()) throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        if (regionMapper.selectOne(new LambdaQueryWrapper<GoldRegionEntity>()
                .eq(GoldRegionEntity::getTenantId, current.tenantId())
                .eq(GoldRegionEntity::getCity, city)
                .eq(GoldRegionEntity::getDeleted, 0)
                .last("LIMIT 1")) != null) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REGION_CONFLICT);
        }

        GoldRegionEntity parent = null;
        String parentText = request == null || request.parentRegionId() == null
                ? ""
                : request.parentRegionId().trim();
        if (!parentText.isBlank()) {
            try {
                parent = regionMapper.selectOne(new LambdaQueryWrapper<GoldRegionEntity>()
                        .eq(GoldRegionEntity::getTenantId, current.tenantId())
                        .eq(GoldRegionEntity::getId, Long.parseLong(parentText))
                        .eq(GoldRegionEntity::getStatus, "ACTIVE")
                        .eq(GoldRegionEntity::getDeleted, 0)
                        .last("LIMIT 1 FOR UPDATE"));
            } catch (NumberFormatException exception) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_REGION_CONFLICT);
            }
            if (parent == null || parent.getDepth() >= 3) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_REGION_CONFLICT);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        GoldRegionEntity region = new GoldRegionEntity();
        region.setId(IdWorker.getId());
        region.setTenantId(current.tenantId());
        region.setOwnerUserId(current.userId());
        region.setCity(city);
        region.setParentRegionId(parent == null ? null : parent.getId());
        region.setDepth(parent == null ? 1 : parent.getDepth() + 1);
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
        return properties.enabled() && properties.developmentMode();
    }

    /** Called by the WeChat provisioning path after a new customer is created. */
    @Transactional
    public void initializeForCustomer(long tenantId, long userId) {
        if (!available()) return;
        if (accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, tenantId)
                .eq(GoldBeanAccountEntity::getUserId, userId)
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1")) != null) return;
        createAccount(tenantId, userId, userId, LocalDateTime.now());
    }

    private CurrentPrincipal requireEnabledCustomer() {
        if (!available()) throw new BusinessException(ErrorCode.GOLD_BEAN_NOT_ENABLED);
        CurrentPrincipal current = CurrentUser.require();
        if (!"CUSTOMER".equals(current.workbench())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        return current;
    }

    private GoldBeanAccountEntity ensureAccount(CurrentPrincipal current) {
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
        account.setReferralCode(createReferralCode(userId));
        account.setRegistrationFeeStatus("UNPAID");
        account.setRegistrationFeeCent(properties.registrationFeeCent());
        account.setDigitalBankBalance(0L);
        account.setTradingBalance(0L);
        account.setDailyRewardStartAt(now);
        account.setDailyRewardDays(0);
        account.setTradeLimitPercent(100);
        account.setStatus("ACTIVE");
        auditNew(account, operatorId, now);
        accountMapper.insert(account);
        return account;
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
        if (account.getDailyRewardDays() >= properties.dailyRewardDays()
                && account.getDirectReferralCount() == 0
                && account.getTradeLimitPercent() != properties.limitedTradePercent()) {
            account.setTradeLimitPercent(properties.limitedTradePercent());
            touch(account, operatorId, LocalDateTime.now());
            accountMapper.updateById(account);
        }
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
            credit(account, properties.dailyRewardBeans(), "DAILY_REWARD", null, null,
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
        GoldBeanLevel current = GoldBeanLevel.fromCode(account.getMemberLevel());
        if (current == GoldBeanLevel.ORDINARY || account.getProtectionUntil() == null
                || !account.getProtectionUntil().isBefore(LocalDateTime.now())) return;

        LocalDate firstDrop = account.getProtectionUntil().toLocalDate().plusDays(1);
        LocalDate nextDrop = account.getLastLevelDropAt() == null
                ? firstDrop
                : account.getLastLevelDropAt().toLocalDate().plusDays(1);
        LocalDate today = LocalDate.now();
        while (current != GoldBeanLevel.ORDINARY && !nextDrop.isAfter(today)) {
            current = previous(current);
            account.setMemberLevel(current.code());
            account.setLastLevelDropAt(nextDrop.atStartOfDay());
            if (current == GoldBeanLevel.ORDINARY) account.setTradeLimitPercent(properties.limitedTradePercent());
            nextDrop = nextDrop.plusDays(1);
        }
        account.setProtectionStartedAt(null);
        account.setProtectionUntil(null);
        account.setLastProtectionReferralAt(null);
        touch(account, operatorId, LocalDateTime.now());
        accountMapper.updateById(account);
    }

    private GoldBeanLevel previous(GoldBeanLevel current) {
        return current == GoldBeanLevel.ORDINARY ? GoldBeanLevel.ORDINARY
                : GoldBeanLevel.values()[current.ordinal() - 1];
    }

    private void processReferralReward(
            GoldBeanAccountEntity referrer,
            GoldBeanAccountEntity referred,
            GoldBeanReferralEntity relation,
            long tenantId,
            LocalDateTime now) {
        settle(referrer, referrer.getUserId());
        referrer.setDirectReferralCount(referrer.getDirectReferralCount() + 1);
        updateLevelAfterReferral(referrer, now);
        activateProtection(referrer, now);
        credit(referrer, DIRECT_REFERRAL_REWARD, "REFERRAL_DIRECT", referred.getUserId(), relation.getId(),
                "直推注册奖励", "REFERRAL:DIRECT:" + relation.getId());

        if (referrer.getReferrerId() != null) {
            GoldBeanAccountEntity parent = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                    .eq(GoldBeanAccountEntity::getTenantId, tenantId)
                    .eq(GoldBeanAccountEntity::getUserId, referrer.getReferrerId())
                    .eq(GoldBeanAccountEntity::getStatus, "ACTIVE")
                    .eq(GoldBeanAccountEntity::getDeleted, 0)
                    .last("LIMIT 1 FOR UPDATE"));
            if (parent != null) {
                credit(parent, SECOND_LEVEL_REFERRAL_REWARD, "REFERRAL_SECOND_LEVEL", referrer.getUserId(), relation.getId(),
                        "二级推荐奖励", "REFERRAL:SECOND:" + relation.getId());
            }
        }
        touch(referrer, referrer.getUserId(), now);
        accountMapper.updateById(referrer);
    }

    private void updateLevelAfterReferral(GoldBeanAccountEntity account, LocalDateTime now) {
        GoldBeanLevel directLevel = GoldBeanLevel.forDirectReferralCount(account.getDirectReferralCount());
        GoldBeanLevel historical = GoldBeanLevel.fromCode(account.getHistoricalLevel());
        GoldBeanLevel current = GoldBeanLevel.fromCode(account.getMemberLevel());
        if (directLevel.ordinal() > historical.ordinal()) {
            for (int ordinal = historical.ordinal() + 1; ordinal <= directLevel.ordinal(); ordinal++) {
                GoldBeanLevel unlocked = GoldBeanLevel.values()[ordinal];
                credit(account, unlocked.unlockReward(), "LEVEL_REWARD", null, null,
                        unlocked.displayName() + "解锁奖励", "LEVEL:" + account.getUserId() + ":" + unlocked.code());
            }
            account.setHistoricalLevel(directLevel.code());
        }
        if (current.ordinal() < historical.ordinal()) {
            account.setMemberLevel(current.next().code());
        } else if (directLevel.ordinal() > current.ordinal()) {
            account.setMemberLevel(directLevel.code());
        }
    }

    private void activateProtection(GoldBeanAccountEntity account, LocalDateTime now) {
        account.setProtectionStartedAt(now);
        account.setProtectionUntil(now.plusDays(properties.protectionDays()));
        account.setLastProtectionReferralAt(now);
        account.setLastLevelDropAt(null);
        account.setTradeLimitPercent(100);
    }

    private void credit(
            GoldBeanAccountEntity account,
            long amount,
            String eventType,
            Long relatedUserId,
            Long relatedReferralId,
            String description,
            String idempotencyPrefix) {
        if (amount <= 0) return;
        long bankAmount = (amount + 1) / 2;
        long tradingAmount = amount / 2;
        String bankKey = idempotencyPrefix + ":BANK";
        String tradingKey = idempotencyPrefix + ":TRADING";
        boolean bankExists = ledgerExists(bankKey);
        boolean tradingExists = ledgerExists(tradingKey);
        if (!bankExists) {
            account.setDigitalBankBalance(account.getDigitalBankBalance() + bankAmount);
            insertLedger(account, "DIGITAL_BANK", bankAmount, eventType, relatedUserId, relatedReferralId,
                    bankKey, description + "（数字银行）");
        }
        if (!tradingExists) {
            account.setTradingBalance(account.getTradingBalance() + tradingAmount);
            insertLedger(account, "TRADING", tradingAmount, eventType, relatedUserId, relatedReferralId,
                    tradingKey, description + "（可交易）");
        }
        if (!bankExists || !tradingExists) accountMapper.updateById(account);
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
            long amount,
            String eventType,
            Long relatedUserId,
            Long relatedReferralId,
            String idempotencyKey,
            String description) {
        LocalDateTime now = LocalDateTime.now();
        GoldBeanLedgerEntity ledger = new GoldBeanLedgerEntity();
        ledger.setId(IdWorker.getId());
        ledger.setTenantId(account.getTenantId());
        ledger.setUserId(account.getUserId());
        ledger.setBucket(bucket);
        ledger.setDirection("CREDIT");
        ledger.setAmount(amount);
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
        GoldRegionEntity region = regionMapper.selectOne(new LambdaQueryWrapper<GoldRegionEntity>()
                .eq(GoldRegionEntity::getTenantId, current.tenantId())
                .eq(GoldRegionEntity::getOwnerUserId, current.userId())
                .eq(GoldRegionEntity::getDeleted, 0)
                .last("LIMIT 1"));
        int dailyDays = account.getDailyRewardDays() == null ? 0 : account.getDailyRewardDays();
        return new GoldBeanSummaryVo(
                true,
                String.valueOf(account.getUserId()),
                account.getReferralCode(),
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
                account.getDigitalBankBalance() + account.getTradingBalance(),
                account.getDigitalBankBalance(),
                account.getTradingBalance(),
                account.getTradeLimitPercent(),
                dailyDays,
                properties.dailyRewardDays(),
                Math.max(0, properties.dailyRewardDays() - dailyDays),
                reminder,
                account.getProtectionUntil(),
                level == GoldBeanLevel.DIAMOND && region == null,
                region == null ? null : String.valueOf(region.getId()),
                region == null ? null : region.getCity());
    }

    private String reminder(GoldBeanAccountEntity account) {
        int days = account.getDailyRewardDays() == null ? 0 : account.getDailyRewardDays();
        if (days >= 15 && days < properties.dailyRewardDays() && account.getDirectReferralCount() == 0) {
            return "你已连续" + (days - 14) + "天没有推荐新人，请在第20天前完成一次直推。";
        }
        if (account.getProtectionStartedAt() != null && account.getProtectionUntil() != null
                && account.getProtectionUntil().isAfter(LocalDateTime.now())) {
            long remaining = Math.max(1, Duration.between(LocalDateTime.now(), account.getProtectionUntil()).toDays());
            if (remaining <= properties.protectionDays() - 3) {
                return "当前等级活跃保护期还剩" + remaining + "天，请及时推荐新人保持等级。";
            }
        }
        if (account.getTradeLimitPercent() < 100) {
            return "当前处于交易额度限制状态，完成一次直推后可恢复100%交易额度。";
        }
        return null;
    }

    private GoldRegionVo toRegion(GoldRegionEntity region) {
        return new GoldRegionVo(
                String.valueOf(region.getId()),
                region.getCity(),
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
