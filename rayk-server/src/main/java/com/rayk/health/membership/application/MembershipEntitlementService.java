package com.rayk.health.membership.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.membership.config.MembershipProperties;
import com.rayk.health.membership.entity.CustomerMembershipEntity;
import com.rayk.health.membership.entity.MembershipBenefitEntity;
import com.rayk.health.membership.entity.MembershipPlanBenefitEntity;
import com.rayk.health.membership.entity.MembershipPlanEntity;
import com.rayk.health.membership.entity.MembershipUsageEntity;
import com.rayk.health.membership.mapper.CustomerMembershipMapper;
import com.rayk.health.membership.mapper.MembershipBenefitMapper;
import com.rayk.health.membership.mapper.MembershipPlanBenefitMapper;
import com.rayk.health.membership.mapper.MembershipPlanMapper;
import com.rayk.health.membership.mapper.MembershipUsageMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Single backend source of truth for membership entitlements and the two-phase usage ledger. */
@Service
public class MembershipEntitlementService {
    public static final String FREE_PLAN_CODE = "FREE_CUSTOMER";
    public static final int FREE_HEALTH_HISTORY_DAYS = 3;

    private final MembershipProperties properties;
    private final MembershipPlanMapper planMapper;
    private final CustomerMembershipMapper membershipMapper;
    private final MembershipBenefitMapper benefitMapper;
    private final MembershipPlanBenefitMapper planBenefitMapper;
    private final MembershipUsageMapper usageMapper;

    public MembershipEntitlementService(
            MembershipProperties properties,
            MembershipPlanMapper planMapper,
            CustomerMembershipMapper membershipMapper,
            MembershipBenefitMapper benefitMapper,
            MembershipPlanBenefitMapper planBenefitMapper,
            MembershipUsageMapper usageMapper) {
        this.properties = properties;
        this.planMapper = planMapper;
        this.membershipMapper = membershipMapper;
        this.benefitMapper = benefitMapper;
        this.planBenefitMapper = planBenefitMapper;
        this.usageMapper = usageMapper;
    }

    public MembershipSnapshot snapshot(CurrentPrincipal current) {
        if (!properties.enabled()) {
            return new MembershipSnapshot(null, null, false, LocalDateTime.now(), List.of());
        }
        CustomerMembershipEntity membership = ensureCurrentMembership(current);
        MembershipPlanEntity plan = planMapper.selectById(membership.getPlanId());
        List<MembershipPlanBenefitEntity> planBenefits = planBenefits(plan.getId());
        return new MembershipSnapshot(membership, plan, isPaidActive(membership, plan),
                LocalDateTime.now(), planBenefits);
    }

    /** Reserve one entitlement. Call confirm after the external service and persistence succeed. */
    @Transactional
    public UsageReservation reserve(
            String benefitCode, String bizType, String bizId, String idempotencyKey) {
        CurrentPrincipal current = CurrentUser.require();
        if (!properties.enabled()) {
            return new UsageReservation(null, true, false);
        }
        String key = idempotencyKey == null || idempotencyKey.isBlank()
                ? benefitCode + ":" + bizType + ":" + Objects.toString(bizId, "") + ":" + UUID.randomUUID()
                : idempotencyKey;
        MembershipUsageEntity existing = usageMapper.selectOne(new LambdaQueryWrapper<MembershipUsageEntity>()
                .eq(MembershipUsageEntity::getIdempotencyKey, key)
                .eq(MembershipUsageEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (existing != null) {
            return new UsageReservation(existing.getId(),
                    "RESERVED".equals(existing.getUsageStatus()) || "CONFIRMED".equals(existing.getUsageStatus()),
                    "CONFIRMED".equals(existing.getUsageStatus()));
        }
        MembershipSnapshot snapshot = snapshot(current);
        if (!allowed(snapshot, benefitCode, bizType, bizId)) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_BENEFIT_NOT_AVAILABLE);
        }
        LocalDateTime now = LocalDateTime.now();
        MembershipUsageEntity usage = new MembershipUsageEntity();
        usage.setTenantId(current.tenantId());
        usage.setCustomerId(current.userId());
        usage.setMembershipId(snapshot.membership() == null ? null : snapshot.membership().getId());
        usage.setBenefitCode(benefitCode);
        usage.setBizType(bizType);
        usage.setBizId(bizId);
        usage.setAmount(1);
        usage.setUsageStatus("RESERVED");
        usage.setIdempotencyKey(key);
        usage.setReservedAt(now);
        usage.setCreatedBy(current.userId());
        usage.setCreatedAt(now);
        usage.setUpdatedBy(current.userId());
        usage.setUpdatedAt(now);
        usage.setDeleted(0);
        usage.setVersion(0);
        try {
            usageMapper.insert(usage);
        } catch (DataIntegrityViolationException duplicate) {
            MembershipUsageEntity winner = usageMapper.selectOne(new LambdaQueryWrapper<MembershipUsageEntity>()
                    .eq(MembershipUsageEntity::getIdempotencyKey, key)
                    .eq(MembershipUsageEntity::getDeleted, 0)
                    .last("LIMIT 1"));
            if (winner != null) {
                return new UsageReservation(winner.getId(),
                        "RESERVED".equals(winner.getUsageStatus()) || "CONFIRMED".equals(winner.getUsageStatus()),
                        "CONFIRMED".equals(winner.getUsageStatus()));
            }
            throw duplicate;
        }
        return new UsageReservation(usage.getId(), true, false);
    }

    @Transactional
    public void confirm(Long usageId) {
        if (usageId == null || !properties.enabled()) return;
        MembershipUsageEntity usage = usageMapper.selectById(usageId);
        if (usage != null && "RESERVED".equals(usage.getUsageStatus())) {
            usage.setUsageStatus("CONFIRMED");
            usage.setConfirmedAt(LocalDateTime.now());
            usage.setUpdatedAt(LocalDateTime.now());
            usageMapper.updateById(usage);
        }
    }

    @Transactional
    public void release(Long usageId) {
        if (usageId == null || !properties.enabled()) return;
        MembershipUsageEntity usage = usageMapper.selectById(usageId);
        if (usage != null && "RESERVED".equals(usage.getUsageStatus())) {
            usage.setUsageStatus("RELEASED");
            usage.setReleasedAt(LocalDateTime.now());
            usage.setUpdatedAt(LocalDateTime.now());
            usageMapper.updateById(usage);
        }
    }

    public boolean allowedForCurrent(String benefitCode, String bizType, String bizId) {
        if (!properties.enabled()) return true;
        return allowed(snapshot(CurrentUser.require()), benefitCode, bizType, bizId);
    }

    /**
     * 免费客户可以查看最近三天的健康拍历史；年度会员可以查看完整历史。
     * 这是查看范围而非消耗型权益，因此不写入 membership_usage。
     */
    public boolean canViewHealthShotHistory(LocalDateTime createdAt) {
        if (!properties.enabled()) return true;
        return canViewHealthShotHistory(snapshot(CurrentUser.require()), createdAt);
    }

    public boolean canViewHealthShotHistory(MembershipSnapshot current, LocalDateTime createdAt) {
        if (!properties.enabled()) return true;
        return (current != null && current.paidActive())
                || (createdAt != null && !createdAt.isBefore(LocalDateTime.now().minusDays(FREE_HEALTH_HISTORY_DAYS)));
    }

    /**
     * 健康拍按会员类型核销：免费客户使用一次性体验额度，年度会员使用每日额度。
     */
    @Transactional
    public HealthShotReservation reserveHealthShot(String taskId, String idempotencyKey) {
        MembershipSnapshot snapshot = snapshot(CurrentUser.require());
        if (!snapshot.paidActive()) {
            return new HealthShotReservation(List.of(reserve(
                    "HEALTH_SHOT", "HEALTH_SCAN", taskId, idempotencyKey + ":lifetime")));
        }
        return new HealthShotReservation(List.of(reserve(
                "HEALTH_SHOT_DAILY", "HEALTH_SCAN", taskId, idempotencyKey + ":daily")));
    }

    public void confirmHealthShot(HealthShotReservation reservation) {
        if (reservation == null) return;
        reservation.usages().forEach(item -> confirm(item.usageId()));
    }

    public void releaseHealthShot(HealthShotReservation reservation) {
        if (reservation == null) return;
        reservation.usages().forEach(item -> release(item.usageId()));
    }

    private boolean allowed(MembershipSnapshot snapshot, String benefitCode, String bizType, String bizId) {
        MembershipBenefitEntity benefit = benefitMapper.selectOne(new LambdaQueryWrapper<MembershipBenefitEntity>()
                .eq(MembershipBenefitEntity::getBenefitCode, benefitCode)
                .eq(MembershipBenefitEntity::getStatus, "ACTIVE")
                .eq(MembershipBenefitEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (benefit == null) return false;
        MembershipPlanBenefitEntity config = planBenefitMapper.selectOne(new LambdaQueryWrapper<MembershipPlanBenefitEntity>()
                .eq(MembershipPlanBenefitEntity::getPlanId, snapshot.plan().getId())
                .eq(MembershipPlanBenefitEntity::getBenefitCode, benefitCode)
                .eq(MembershipPlanBenefitEntity::getEnabled, true)
                .eq(MembershipPlanBenefitEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (config == null) return false;
        int used = usageCount(snapshot, benefitCode, bizType, bizId);
        if ("ENABLED".equals(benefit.getUnitType())) return snapshot.paidActive();
        Integer quota = effectiveQuota(snapshot, config);
        return quota == null || used < quota;
    }

    /** 环境变量只覆盖免费体验额度；年度会员额度仍以数据库方案配置为准。 */
    public Integer effectiveQuota(MembershipSnapshot snapshot, MembershipPlanBenefitEntity config) {
        if (snapshot == null || snapshot.plan() == null || config == null
                || !FREE_PLAN_CODE.equals(snapshot.plan().getPlanCode())) {
            return config == null ? null : config.getQuotaValue();
        }
        return switch (config.getBenefitCode()) {
            case "AI_HEALTH_ASSESSMENT" -> properties.freeAiAssessmentTrial();
            case "AI_HEALTH_REPORT" -> properties.freeAiReportTrial();
            case "AI_FOLLOWUP_INITIAL" -> properties.freeAiFollowupTrial();
            case "HEALTH_SHOT" -> properties.freeHealthShotTrial();
            case "TTS_MEAL_REMINDER" -> properties.freeTtsMealTrial();
            case "TTS_SLEEP_REMINDER" -> properties.freeTtsSleepTrial();
            default -> config.getQuotaValue();
        };
    }

    public int usageCount(MembershipSnapshot snapshot, String benefitCode, String bizType, String bizId) {
        LambdaQueryWrapper<MembershipUsageEntity> query = new LambdaQueryWrapper<MembershipUsageEntity>()
                .eq(MembershipUsageEntity::getTenantId, snapshot.membership().getTenantId())
                .eq(MembershipUsageEntity::getCustomerId, snapshot.membership().getCustomerId())
                .in(MembershipUsageEntity::getUsageStatus, List.of("RESERVED", "CONFIRMED"))
                .eq(MembershipUsageEntity::getDeleted, 0);
        if ("AI_HEALTH_ASSESSMENT".equals(benefitCode)) {
            query.and(wrapper -> wrapper.eq(MembershipUsageEntity::getBenefitCode, benefitCode)
                    .or(old -> old.eq(MembershipUsageEntity::getBenefitCode, "AI_HEALTH_REPORT")
                            .eq(MembershipUsageEntity::getBizType, "HEALTH_ASSESSMENT")));
        } else {
            query.eq(MembershipUsageEntity::getBenefitCode, benefitCode);
        }
        MembershipBenefitEntity benefit = benefitMapper.selectOne(new LambdaQueryWrapper<MembershipBenefitEntity>()
                .eq(MembershipBenefitEntity::getBenefitCode, benefitCode).last("LIMIT 1"));
        if ("PER_REPORT".equals(benefit == null ? null : benefit.getUnitType())) {
            query.eq(MembershipUsageEntity::getBizId, bizId);
        } else if (snapshot.paidActive() && snapshot.membership() != null) {
            query.ge(MembershipUsageEntity::getCreatedAt, snapshot.membership().getStartAt())
                    .le(MembershipUsageEntity::getCreatedAt, snapshot.membership().getExpireAt());
        }
        String unit = benefit == null ? "" : benefit.getUnitType();
        LocalDateTime now = LocalDateTime.now();
        if ("DAILY".equals(unit)) query.ge(MembershipUsageEntity::getCreatedAt, now.toLocalDate().atStartOfDay());
        if ("ROLLING_30D".equals(unit)) query.ge(MembershipUsageEntity::getCreatedAt, now.minusDays(30));
        return Math.toIntExact(usageMapper.selectCount(query));
    }

    public MembershipPlanEntity plan(String code) {
        return planMapper.selectOne(new LambdaQueryWrapper<MembershipPlanEntity>()
                .eq(MembershipPlanEntity::getPlanCode, code).eq(MembershipPlanEntity::getDeleted, 0).last("LIMIT 1"));
    }

    public CustomerMembershipEntity ensureCurrentMembership(CurrentPrincipal current) {
        LocalDateTime now = LocalDateTime.now();
        CustomerMembershipEntity active = membershipMapper.selectOne(new LambdaQueryWrapper<CustomerMembershipEntity>()
                .eq(CustomerMembershipEntity::getTenantId, current.tenantId())
                .eq(CustomerMembershipEntity::getCustomerId, current.userId())
                .eq(CustomerMembershipEntity::getStatus, "ACTIVE")
                .gt(CustomerMembershipEntity::getExpireAt, now)
                .eq(CustomerMembershipEntity::getDeleted, 0)
                // 免费会员永久有效，不能按 expire_at 排序，否则会遮蔽刚开通的年度会员。
                .orderByDesc(CustomerMembershipEntity::getStartAt)
                .orderByDesc(CustomerMembershipEntity::getExpireAt)
                .last("LIMIT 1"));
        if (active != null) return active;
        MembershipPlanEntity free = plan(FREE_PLAN_CODE);
        CustomerMembershipEntity existingFree = membershipMapper.selectOne(new LambdaQueryWrapper<CustomerMembershipEntity>()
                .eq(CustomerMembershipEntity::getTenantId, current.tenantId())
                .eq(CustomerMembershipEntity::getCustomerId, current.userId())
                .eq(CustomerMembershipEntity::getPlanId, free.getId())
                .eq(CustomerMembershipEntity::getDeleted, 0).last("LIMIT 1"));
        if (existingFree != null) return existingFree;
        CustomerMembershipEntity created = new CustomerMembershipEntity();
        created.setTenantId(current.tenantId());
        created.setCustomerId(current.userId());
        created.setPlanId(free.getId());
        created.setStatus("ACTIVE");
        created.setStartAt(now);
        created.setExpireAt(LocalDateTime.of(9999, 12, 31, 23, 59, 59));
        created.setSource("SYSTEM");
        created.setCreatedBy(current.userId());
        created.setCreatedAt(now);
        created.setUpdatedBy(current.userId());
        created.setUpdatedAt(now);
        created.setDeleted(0);
        created.setVersion(0);
        membershipMapper.insert(created);
        return created;
    }

    private List<MembershipPlanBenefitEntity> planBenefits(Long planId) {
        return planBenefitMapper.selectList(new LambdaQueryWrapper<MembershipPlanBenefitEntity>()
                .eq(MembershipPlanBenefitEntity::getPlanId, planId)
                .eq(MembershipPlanBenefitEntity::getEnabled, true)
                .eq(MembershipPlanBenefitEntity::getDeleted, 0));
    }

    private boolean isPaidActive(CustomerMembershipEntity membership, MembershipPlanEntity plan) {
        return membership != null && plan != null && !FREE_PLAN_CODE.equals(plan.getPlanCode())
                && "ACTIVE".equals(membership.getStatus()) && membership.getExpireAt().isAfter(LocalDateTime.now());
    }

    public record MembershipSnapshot(
            CustomerMembershipEntity membership,
            MembershipPlanEntity plan,
            boolean paidActive,
            LocalDateTime now,
            List<MembershipPlanBenefitEntity> planBenefits) {}

    public record UsageReservation(Long usageId, boolean allowed, boolean alreadyConfirmed) {}

    public record HealthShotReservation(List<UsageReservation> usages) {}
}
