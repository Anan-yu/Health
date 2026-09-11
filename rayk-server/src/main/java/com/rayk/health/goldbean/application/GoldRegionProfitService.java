package com.rayk.health.goldbean.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldRegionEntity;
import com.rayk.health.goldbean.entity.GoldRegionProfitDistributionEntity;
import com.rayk.health.goldbean.entity.GoldRegionProfitEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldRegionMapper;
import com.rayk.health.goldbean.util.GoldBeanAmounts;
import com.rayk.health.goldbean.mapper.GoldRegionProfitDistributionMapper;
import com.rayk.health.goldbean.mapper.GoldRegionProfitMapper;
import com.rayk.health.platform.vo.PlatformGoldRegionProfitDistributionVo;
import com.rayk.health.platform.vo.PlatformGoldRegionProfitVo;
import com.rayk.health.platform.vo.PlatformGoldRegionVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.system.entity.SysTenantEntity;
import com.rayk.health.system.entity.SysUserEntity;
import com.rayk.health.system.mapper.SysTenantMapper;
import com.rayk.health.system.mapper.SysUserMapper;
import com.rayk.health.tenant.TenantContext;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

    /** Reports regions and distributes a direct-upline rebate based on the region owner's own rewards. */
@Service
public class GoldRegionProfitService {
    private static final String AUTO_DISTRIBUTED = "AUTO_DISTRIBUTED";
    private static final String REGION_REWARD = "REGION_REWARD";
    private static final String OWNER = "OWNER";
    private static final String DIRECT_UPLINE = "DIRECT_UPLINE";
    private static final String GRAND_UPLINE = "GRAND_UPLINE";

    private final GoldBeanProperties properties;
    private final GoldBeanAccountMapper accountMapper;
    private final GoldRegionMapper regionMapper;
    private final GoldRegionProfitMapper profitMapper;
    private final GoldRegionProfitDistributionMapper distributionMapper;
    private final GoldBeanApplicationService goldBeanService;
    private final SysUserMapper userMapper;
    private final SysTenantMapper tenantMapper;

    public GoldRegionProfitService(
            GoldBeanProperties properties,
            GoldBeanAccountMapper accountMapper,
            GoldRegionMapper regionMapper,
            GoldRegionProfitMapper profitMapper,
            GoldRegionProfitDistributionMapper distributionMapper,
            GoldBeanApplicationService goldBeanService,
            SysUserMapper userMapper,
            SysTenantMapper tenantMapper) {
        this.properties = properties;
        this.accountMapper = accountMapper;
        this.regionMapper = regionMapper;
        this.profitMapper = profitMapper;
        this.distributionMapper = distributionMapper;
        this.goldBeanService = goldBeanService;
        this.userMapper = userMapper;
        this.tenantMapper = tenantMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformGoldRegionVo> regions() {
        requirePlatformAdmin();
        return TenantContext.executeReadWithoutTenant(() -> {
            List<GoldRegionEntity> regions = regionMapper.selectList(new LambdaQueryWrapper<GoldRegionEntity>()
                    .eq(GoldRegionEntity::getDeleted, 0)
                    .ne(GoldRegionEntity::getTenantId, 1L)
                    .orderByDesc(GoldRegionEntity::getCreatedAt)
                    .last("LIMIT 200"));
            RegionContext context = regionContext(regions);
            List<PlatformGoldRegionVo> records = regions.stream()
                    .map(region -> toRegion(region, context.accounts(), context.users(), context.tenants()))
                    .toList();
            return new PageResponse<>(records, records.size(), 1, Math.max(records.size(), 1));
        });
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformGoldRegionProfitVo> profits() {
        requirePlatformAdmin();
        return TenantContext.executeReadWithoutTenant(() -> {
            List<GoldRegionProfitEntity> profits = profitMapper.selectList(
                    new LambdaQueryWrapper<GoldRegionProfitEntity>()
                            .eq(GoldRegionProfitEntity::getDeleted, 0)
                            .ne(GoldRegionProfitEntity::getTenantId, 1L)
                            .orderByDesc(GoldRegionProfitEntity::getSettledAt)
                            .last("LIMIT 200"));
            List<GoldRegionEntity> regions = regionsForProfits(profits);
            RegionContext context = regionContext(regions);
            Map<Long, GoldRegionEntity> regionById = regions.stream()
                    .filter(item -> item.getId() != null)
                    .collect(Collectors.toMap(GoldRegionEntity::getId, Function.identity(), (left, right) -> left));
            List<PlatformGoldRegionProfitVo> records = profits.stream()
                    .map(item -> toProfit(item, regionById.get(item.getRegionId()), context.users(), context.tenants()))
                    .toList();
            return new PageResponse<>(records, records.size(), 1, Math.max(records.size(), 1));
        });
    }

    /**
     * Automatically allocates a direct-upline rebate when the owner of the nearest active region
     * receives a referral-registration reward. The recruited agent is identified through
     * {@code relatedUserId}; rewards paid to other accounts in the same registration chain are
     * not regional rebate bases. The recruited agent must also belong to the region's city.
     */
    @Transactional
    void distributeForReward(
            GoldBeanAccountEntity rewardedAccount,
            BigDecimal amount,
            String eventType,
            Long relatedUserId,
            String sourceIdempotencyKey) {
        if (!isReferralRegistrationReward(eventType)
                || rewardedAccount == null
                || relatedUserId == null
                || !GoldBeanAmounts.positive(amount)
                || !StringUtils.hasText(sourceIdempotencyKey)) {
            return;
        }

        GoldBeanAccountEntity generatedBy = rewardSubject(rewardedAccount, relatedUserId);
        if (!eligibleRecipient(generatedBy)) return;
        RegionTarget target = findRegionForSubject(generatedBy);
        if (target == null) return;

        GoldBeanAccountEntity owner = target.owner();
        if (!Objects.equals(rewardedAccount.getUserId(), owner.getUserId())) return;
        if (!sameCity(generatedBy.getCity(), target.region().getCity())) return;
        List<GoldBeanAccountEntity> ancestors = goldBeanService.loadRegionAncestors(owner, true);
        if (ancestors.size() > 2) return;
        if (ancestors.isEmpty()) return;

        GoldRegionEntity region = target.region();
        String idempotencyKey = "REGION_AUTO:" + sourceIdempotencyKey.trim() + ":" + region.getId();
        GoldRegionProfitEntity previous = profitMapper.selectOne(new LambdaQueryWrapper<GoldRegionProfitEntity>()
                .eq(GoldRegionProfitEntity::getIdempotencyKey, idempotencyKey)
                .eq(GoldRegionProfitEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (previous != null) return;

        List<Allocation> allocations = new ArrayList<>();
        if (ancestors.size() >= 1) {
            int directRate = 20;
            allocations.add(new Allocation(ancestors.get(0), DIRECT_UPLINE, directRate,
                    percentage(amount, directRate)));
        }

        LocalDateTime now = LocalDateTime.now();
        GoldRegionProfitEntity profit = new GoldRegionProfitEntity();
        profit.setId(IdWorker.getId());
        profit.setTenantId(region.getTenantId());
        profit.setRegionId(region.getId());
        profit.setOwnerUserId(owner.getUserId());
        BigDecimal normalizedAmount = GoldBeanAmounts.normalize(amount);
        profit.setAmount(normalizedAmount);
        profit.setOwnerAmount(GoldBeanAmounts.ZERO);
        profit.setRetainedAmount(GoldBeanAmounts.ZERO);
        profit.setStatus(AUTO_DISTRIBUTED);
        profit.setIdempotencyKey(idempotencyKey);
        profit.setSettledAt(now);
        auditNew(profit, 0L, now);
        try {
            profitMapper.insert(profit);
        } catch (DuplicateKeyException exception) {
            // Another request has already distributed this source reward.
            return;
        }

        BigDecimal retainedAmount = GoldBeanAmounts.ZERO;
        for (Allocation allocation : allocations) {
            BigDecimal allocationAmount = allocation.amount();
            if (!GoldBeanAmounts.positive(allocationAmount)) continue;
            if (!eligibleRecipient(allocation.account())) {
                retainedAmount = retainedAmount.add(allocationAmount);
                continue;
            }
            String ledgerKey = "REGION_REWARD:" + profit.getId() + ":" + allocation.account().getUserId();
            goldBeanService.creditRegionProfit(
                    allocation.account(), allocationAmount, generatedBy.getUserId(), ledgerKey,
                    description(region, allocation));
            GoldRegionProfitDistributionEntity distribution = new GoldRegionProfitDistributionEntity();
            distribution.setId(IdWorker.getId());
            distribution.setTenantId(region.getTenantId());
            distribution.setProfitId(profit.getId());
            distribution.setRegionId(region.getId());
            distribution.setRecipientUserId(allocation.account().getUserId());
            distribution.setRecipientType(allocation.recipientType());
            distribution.setRatePercent(allocation.ratePercent());
            distribution.setAmount(allocationAmount);
            distribution.setIdempotencyKey(ledgerKey);
            auditNew(distribution, 0L, now);
            distributionMapper.insert(distribution);
        }

        // The owner already received the source referral reward in GoldBeanApplicationService;
        // only the upstream rebate is an additional regional distribution.
        profit.setOwnerAmount(GoldBeanAmounts.ZERO);
        profit.setRetainedAmount(retainedAmount);
        profitMapper.updateById(profit);
    }

    private GoldBeanAccountEntity rewardSubject(
            GoldBeanAccountEntity rewardedAccount, Long relatedUserId) {
        if (relatedUserId == null || Objects.equals(relatedUserId, rewardedAccount.getUserId())) {
            return rewardedAccount;
        }
        return findAccountForUpdate(rewardedAccount.getTenantId(), relatedUserId);
    }

    private RegionTarget findRegionForSubject(GoldBeanAccountEntity subject) {
        Set<Long> visited = new HashSet<>();
        Long ancestorId = subject.getReferrerId();
        int hops = 0;
        while (ancestorId != null && hops < 3 && visited.add(ancestorId)) {
            GoldBeanAccountEntity ancestor = findAccountForUpdate(subject.getTenantId(), ancestorId);
            if (ancestor == null) return null;
            GoldRegionEntity region = regionMapper.selectOne(new LambdaQueryWrapper<GoldRegionEntity>()
                    .eq(GoldRegionEntity::getTenantId, subject.getTenantId())
                    .eq(GoldRegionEntity::getOwnerUserId, ancestor.getUserId())
                    .eq(GoldRegionEntity::getStatus, "ACTIVE")
                    .eq(GoldRegionEntity::getDeleted, 0)
                    .last("LIMIT 1 FOR UPDATE"));
            if (region != null) return new RegionTarget(region, ancestor);
            ancestorId = ancestor.getReferrerId();
            hops++;
        }
        return null;
    }

    private static boolean isReferralRegistrationReward(String eventType) {
        return switch (eventType) {
            case "REFERRAL_DIRECT", "REFERRAL_DOWNLINE" -> true;
            default -> false;
        };
    }

    private static boolean sameCity(String registeredCity, String regionCity) {
        if (!StringUtils.hasText(registeredCity) || !StringUtils.hasText(regionCity)) return false;
        return registeredCity.trim().replaceAll("\\s+", " ")
                .equals(regionCity.trim().replaceAll("\\s+", " "));
    }

    private List<GoldRegionEntity> regionsForProfits(List<GoldRegionProfitEntity> profits) {
        if (profits.isEmpty()) return List.of();
        List<Long> ids = profits.stream().map(GoldRegionProfitEntity::getRegionId).filter(Objects::nonNull).toList();
        return regionMapper.selectList(new LambdaQueryWrapper<GoldRegionEntity>()
                .in(GoldRegionEntity::getId, ids)
                .eq(GoldRegionEntity::getDeleted, 0));
    }

    private RegionContext regionContext(List<GoldRegionEntity> regions) {
        List<GoldBeanAccountEntity> accounts = accountMapper.selectList(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .ne(GoldBeanAccountEntity::getTenantId, 1L));
        return new RegionContext(accounts, loadUsers(), loadTenants());
    }

    private GoldRegionEntity findRegion(long regionId) {
        return regionMapper.selectOne(new LambdaQueryWrapper<GoldRegionEntity>()
                .eq(GoldRegionEntity::getId, regionId)
                .eq(GoldRegionEntity::getDeleted, 0)
                .last("LIMIT 1"));
    }

    private GoldBeanAccountEntity findAccountForUpdate(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) return null;
        return accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, tenantId)
                .eq(GoldBeanAccountEntity::getUserId, userId)
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
    }

    private PlatformGoldRegionVo toRegion(
            GoldRegionEntity region,
            List<GoldBeanAccountEntity> accounts,
            Map<Long, SysUserEntity> users,
            Map<Long, SysTenantEntity> tenants) {
        GoldBeanAccountEntity account = accounts.stream()
                .filter(item -> Objects.equals(item.getTenantId(), region.getTenantId())
                        && Objects.equals(item.getUserId(), region.getOwnerUserId()))
                .findFirst()
                .orElse(null);
        SysUserEntity user = users.get(region.getOwnerUserId());
        return new PlatformGoldRegionVo(
                id(region.getId()),
                id(region.getTenantId()),
                tenantName(tenants.get(region.getTenantId())),
                region.getCity(),
                safeInt(region.getDepth()),
                id(region.getParentRegionId()),
                id(region.getOwnerUserId()),
                userName(user),
                phone(user),
                account == null ? "普通会员" : GoldBeanLevel.fromCode(account.getMemberLevel()).displayName(),
                region.getStatus(),
                region.getCreatedAt());
    }

    private PlatformGoldRegionProfitVo toProfit(
            GoldRegionProfitEntity profit,
            GoldRegionEntity region,
            Map<Long, SysUserEntity> users,
            Map<Long, SysTenantEntity> tenants) {
        List<PlatformGoldRegionProfitDistributionVo> distributions = distributionMapper.selectList(
                        new LambdaQueryWrapper<GoldRegionProfitDistributionEntity>()
                                .eq(GoldRegionProfitDistributionEntity::getProfitId, profit.getId())
                                .eq(GoldRegionProfitDistributionEntity::getDeleted, 0)
                                .orderByAsc(GoldRegionProfitDistributionEntity::getRecipientType))
                .stream()
                .map(item -> new PlatformGoldRegionProfitDistributionVo(
                        id(item.getRecipientUserId()),
                        userName(users.get(item.getRecipientUserId())),
                        item.getRecipientType(),
                        safeInt(item.getRatePercent()),
                        safeAmount(item.getAmount())))
                .toList();
        return new PlatformGoldRegionProfitVo(
                id(profit.getId()),
                id(profit.getTenantId()),
                tenantName(tenants.get(profit.getTenantId())),
                id(profit.getRegionId()),
                region == null ? "区域已不存在" : region.getCity(),
                region == null ? 0 : safeInt(region.getDepth()),
                id(profit.getOwnerUserId()),
                userName(users.get(profit.getOwnerUserId())),
                safeAmount(profit.getAmount()),
                safeAmount(profit.getOwnerAmount()),
                safeAmount(profit.getRetainedAmount()),
                profit.getStatus(),
                profit.getIdempotencyKey(),
                profit.getSettledAt(),
                distributions);
    }

    private Map<Long, SysUserEntity> loadUsers() {
        return userMapper.selectAllIgnoringTenant().stream()
                .collect(Collectors.toMap(SysUserEntity::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, SysTenantEntity> loadTenants() {
        return tenantMapper.selectList(new LambdaQueryWrapper<SysTenantEntity>()
                        .eq(SysTenantEntity::getDeleted, 0)
                        .ne(SysTenantEntity::getTenantId, 1L))
                .stream()
                .collect(Collectors.toMap(SysTenantEntity::getTenantId, Function.identity(), (left, right) -> left));
    }

    private CurrentPrincipal requirePlatformAdmin() {
        CurrentPrincipal current = CurrentUser.require();
        if (!current.roles().contains("PLATFORM_ADMIN")) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        if (!properties.enabled()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_NOT_ENABLED);
        }
        return current;
    }

    private static boolean eligibleRecipient(GoldBeanAccountEntity account) {
        return account != null
                && "PAID".equals(account.getRegistrationFeeStatus())
                && "ACTIVE".equals(account.getStatus());
    }

    private static BigDecimal percentage(BigDecimal amount, int rate) {
        return GoldBeanAmounts.percentage(amount, rate);
    }

    private static String description(GoldRegionEntity region, Allocation allocation) {
        String share = allocation.ratePercent() + "%";
        return switch (allocation.recipientType()) {
            case OWNER -> "区域" + region.getCity() + "代理奖励返利（开辟人" + share + "）";
            case DIRECT_UPLINE -> "区域" + region.getCity() + "代理奖励返利（直接上级额外" + share + "）";
            case GRAND_UPLINE -> "区域" + region.getCity() + "代理奖励返利（祖级上级额外" + share + "）";
            default -> "区域" + region.getCity() + "代理奖励返利";
        };
    }

    private static void auditNew(Object entity, long userId, LocalDateTime now) {
        if (entity instanceof GoldRegionProfitEntity value) {
            value.setCreatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedBy(userId);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        } else if (entity instanceof GoldRegionProfitDistributionEntity value) {
            value.setCreatedBy(userId);
            value.setCreatedAt(now);
            value.setUpdatedBy(userId);
            value.setUpdatedAt(now);
            value.setDeleted(0);
            value.setVersion(0);
        }
    }

    private static String id(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private static BigDecimal safeAmount(BigDecimal value) {
        return GoldBeanAmounts.nonNegative(value);
    }

    private static String userName(SysUserEntity user) {
        return user == null || !StringUtils.hasText(user.getDisplayName()) ? "未命名用户" : user.getDisplayName();
    }

    private static String phone(SysUserEntity user) {
        return user == null || !StringUtils.hasText(user.getPhoneMasked()) ? "手机号已隐藏" : user.getPhoneMasked();
    }

    private static String tenantName(SysTenantEntity tenant) {
        return tenant == null || !StringUtils.hasText(tenant.getTenantName()) ? "未命名机构" : tenant.getTenantName();
    }

    private record Allocation(
            GoldBeanAccountEntity account, String recipientType, int ratePercent, BigDecimal amount) {}

    private record RegionTarget(GoldRegionEntity region, GoldBeanAccountEntity owner) {}

    private record RegionContext(
            List<GoldBeanAccountEntity> accounts,
            Map<Long, SysUserEntity> users,
            Map<Long, SysTenantEntity> tenants) {}
}
