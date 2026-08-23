package com.rayk.health.platform.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.membership.application.MembershipEntitlementService;
import com.rayk.health.membership.entity.CustomerMembershipEntity;
import com.rayk.health.membership.entity.MembershipPlanEntity;
import com.rayk.health.membership.mapper.CustomerMembershipMapper;
import com.rayk.health.membership.mapper.MembershipPlanMapper;
import com.rayk.health.platform.dto.UpdatePlatformCustomerMembershipRequest;
import com.rayk.health.platform.vo.PlatformCustomerMembershipVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.wechat.PhoneIdentity;
import com.rayk.health.system.entity.SysUserEntity;
import com.rayk.health.system.mapper.SysUserMapper;
import com.rayk.health.system.mapper.SysUserRoleMapper;
import com.rayk.health.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cross-tenant membership operations reserved for the platform administrator. */
@Service
public class PlatformMembershipService {
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final CustomerMembershipMapper membershipMapper;
    private final MembershipPlanMapper planMapper;

    public PlatformMembershipService(
            SysUserMapper userMapper,
            SysUserRoleMapper userRoleMapper,
            CustomerMembershipMapper membershipMapper,
            MembershipPlanMapper planMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.membershipMapper = membershipMapper;
        this.planMapper = planMapper;
    }

    @Transactional(readOnly = true)
    public PlatformCustomerMembershipVo findByPhone(String phone) {
        requirePlatformAdmin();
        SysUserEntity customer = findCustomer(phone);
        return inTenant(customer.getTenantId(), () -> snapshot(customer, LocalDateTime.now()));
    }

    @Transactional
    public PlatformCustomerMembershipVo update(UpdatePlatformCustomerMembershipRequest request) {
        CurrentPrincipal admin = requirePlatformAdmin();
        SysUserEntity customer = findCustomer(request.phone());
        return inTenant(customer.getTenantId(), () -> {
            LocalDateTime now = LocalDateTime.now();
            MembershipPlanEntity freePlan = requiredPlan(MembershipEntitlementService.FREE_PLAN_CODE);
            MembershipPlanEntity yearlyPlan = requiredPlan("AI_HEALTH_YEARLY");
            List<CustomerMembershipEntity> activeMemberships = membershipMapper.selectList(
                    new LambdaQueryWrapper<CustomerMembershipEntity>()
                            .eq(CustomerMembershipEntity::getTenantId, customer.getTenantId())
                            .eq(CustomerMembershipEntity::getCustomerId, customer.getId())
                            .eq(CustomerMembershipEntity::getStatus, "ACTIVE")
                            .eq(CustomerMembershipEntity::getDeleted, 0)
                            .orderByDesc(CustomerMembershipEntity::getStartAt)
                            .orderByDesc(CustomerMembershipEntity::getExpireAt));

            CustomerMembershipEntity activePaid = activeMemberships.stream()
                    .filter(item -> !freePlan.getId().equals(item.getPlanId()))
                    .filter(item -> item.getExpireAt() != null && item.getExpireAt().isAfter(now))
                    .findFirst()
                    .orElse(null);

            if (Boolean.TRUE.equals(request.active())) {
                if (activePaid != null) {
                    return snapshot(customer, now);
                }
                expirePaidMemberships(activeMemberships, freePlan.getId(), admin.userId(), now);
                CustomerMembershipEntity membership = new CustomerMembershipEntity();
                membership.setId(IdWorker.getId());
                membership.setTenantId(customer.getTenantId());
                membership.setCustomerId(customer.getId());
                membership.setPlanId(yearlyPlan.getId());
                membership.setStatus("ACTIVE");
                membership.setStartAt(now);
                membership.setExpireAt(now.plusDays(yearlyPlan.getDurationDays()));
                membership.setSource("PLATFORM_ADMIN");
                membership.setCreatedBy(admin.userId());
                membership.setCreatedAt(now);
                membership.setUpdatedBy(admin.userId());
                membership.setUpdatedAt(now);
                membership.setDeleted(0);
                membership.setVersion(0);
                membershipMapper.insert(membership);
            } else {
                expirePaidMemberships(activeMemberships, freePlan.getId(), admin.userId(), now);
            }
            return snapshot(customer, now);
        });
    }

    private void expirePaidMemberships(
            List<CustomerMembershipEntity> memberships, Long freePlanId, long operatorId, LocalDateTime now) {
        memberships.stream()
                .filter(item -> !freePlanId.equals(item.getPlanId()))
                .forEach(item -> {
                    item.setStatus("EXPIRED");
                    item.setExpireAt(now);
                    item.setUpdatedBy(operatorId);
                    item.setUpdatedAt(now);
                    membershipMapper.updateById(item);
                });
    }

    private PlatformCustomerMembershipVo snapshot(SysUserEntity customer, LocalDateTime now) {
        MembershipPlanEntity freePlan = requiredPlan(MembershipEntitlementService.FREE_PLAN_CODE);
        CustomerMembershipEntity active = membershipMapper.selectOne(
                new LambdaQueryWrapper<CustomerMembershipEntity>()
                        .eq(CustomerMembershipEntity::getTenantId, customer.getTenantId())
                        .eq(CustomerMembershipEntity::getCustomerId, customer.getId())
                        .eq(CustomerMembershipEntity::getStatus, "ACTIVE")
                        .gt(CustomerMembershipEntity::getExpireAt, now)
                        .eq(CustomerMembershipEntity::getDeleted, 0)
                        .orderByDesc(CustomerMembershipEntity::getStartAt)
                        .orderByDesc(CustomerMembershipEntity::getExpireAt)
                        .last("LIMIT 1"));
        if (active == null || freePlan.getId().equals(active.getPlanId())) {
            return new PlatformCustomerMembershipVo(
                    String.valueOf(customer.getId()), customer.getDisplayName(), customer.getPhoneMasked(),
                    "FREE", freePlan.getPlanName(), false, null);
        }
        MembershipPlanEntity plan = planMapper.selectById(active.getPlanId());
        return new PlatformCustomerMembershipVo(
                String.valueOf(customer.getId()), customer.getDisplayName(), customer.getPhoneMasked(),
                "ACTIVE", plan == null ? "年度健康会员" : plan.getPlanName(), true, active.getExpireAt());
    }

    private MembershipPlanEntity requiredPlan(String code) {
        MembershipPlanEntity plan = planMapper.selectOne(new LambdaQueryWrapper<MembershipPlanEntity>()
                .eq(MembershipPlanEntity::getPlanCode, code)
                .eq(MembershipPlanEntity::getStatus, "ACTIVE")
                .eq(MembershipPlanEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (plan == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return plan;
    }

    private SysUserEntity findCustomer(String phone) {
        String normalized;
        try {
            normalized = PhoneIdentity.normalize(phone);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        SysUserEntity customer = userMapper.selectByPhoneHashIgnoringTenant(PhoneIdentity.hash(normalized));
        if (customer == null || !"ACTIVE".equals(customer.getStatus())
                || !userRoleMapper.selectRoleCodesByUserId(customer.getId()).contains("CUSTOMER")) {
            throw new BusinessException(ErrorCode.PLATFORM_CUSTOMER_NOT_FOUND);
        }
        return customer;
    }

    private CurrentPrincipal requirePlatformAdmin() {
        CurrentPrincipal current = CurrentUser.require();
        if (!current.roles().contains("PLATFORM_ADMIN")) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        return current;
    }

    private <T> T inTenant(long tenantId, TenantAction<T> action) {
        Long previous = TenantContext.get();
        try {
            TenantContext.set(tenantId);
            return action.run();
        } finally {
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(previous);
            }
        }
    }

    @FunctionalInterface
    private interface TenantAction<T> {
        T run();
    }
}
