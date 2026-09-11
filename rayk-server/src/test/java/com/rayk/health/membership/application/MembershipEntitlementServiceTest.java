package com.rayk.health.membership.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MembershipEntitlementServiceTest {
    @Test
    void freeCustomerCanUseTreeHoleDuringSevenDayTrial() {
        Fixtures fixtures = new Fixtures(null);

        assertThat(
                        fixtures.service.allowedForSnapshot(
                                fixtures.snapshot(false),
                                MembershipEntitlementService.HEALTH_TREE_HOLE_BENEFIT_CODE,
                                "HEALTH_TREE_HOLE",
                                "conversation-1"))
                .isTrue();
    }

    @Test
    void expiredFreeTrialAllowsOnlyTheFirstFeedbackGeneration() {
        MembershipUsageEntity firstUse = new MembershipUsageEntity();
        firstUse.setCreatedAt(LocalDateTime.now().minusDays(8));
        Fixtures fixtures = new Fixtures(firstUse);

        assertThat(
                        fixtures.service.allowedForSnapshot(
                                fixtures.snapshot(false),
                                MembershipEntitlementService.HEALTH_TREE_HOLE_BENEFIT_CODE,
                                "HEALTH_TREE_HOLE",
                                "conversation-1"))
                .isFalse();
        assertThat(
                        fixtures.service.allowedForSnapshot(
                                fixtures.snapshot(false),
                                MembershipEntitlementService.HEALTH_TREE_HOLE_BENEFIT_CODE,
                                MembershipEntitlementService.HEALTH_TREE_HOLE_FEEDBACK_BIZ_TYPE,
                                "patient-1:period-1"))
                .isTrue();

        when(fixtures.usageMapper.selectCount(any())).thenReturn(1L);
        assertThat(
                        fixtures.service.allowedForSnapshot(
                                fixtures.snapshot(false),
                                MembershipEntitlementService.HEALTH_TREE_HOLE_BENEFIT_CODE,
                                MembershipEntitlementService.HEALTH_TREE_HOLE_FEEDBACK_BIZ_TYPE,
                                "patient-1:period-1"))
                .isFalse();
    }

    @Test
    void activeHealthMemberCanUseTreeHoleWithoutTrialLimit() {
        Fixtures fixtures = new Fixtures(null);

        assertThat(
                        fixtures.service.allowedForSnapshot(
                                fixtures.snapshot(true),
                                MembershipEntitlementService.HEALTH_TREE_HOLE_BENEFIT_CODE,
                                "HEALTH_TREE_HOLE",
                                "conversation-1"))
                .isTrue();
    }

    private static final class Fixtures {
        private final MembershipUsageMapper usageMapper = mock(MembershipUsageMapper.class);
        private final MembershipEntitlementService service;
        private final MembershipPlanEntity plan;
        private final CustomerMembershipEntity membership;

        private Fixtures(MembershipUsageEntity firstUse) {
            MembershipProperties properties =
                    new MembershipProperties(
                            true,
                            false,
                            3,
                            3,
                            1,
                            3,
                            3,
                            3,
                            false,
                            MembershipProperties.WeChatPayProperties.empty(),
                            MembershipProperties.WeChatVirtualPayProperties.empty());
            MembershipPlanMapper planMapper = mock(MembershipPlanMapper.class);
            CustomerMembershipMapper membershipMapper = mock(CustomerMembershipMapper.class);
            MembershipBenefitMapper benefitMapper = mock(MembershipBenefitMapper.class);
            MembershipPlanBenefitMapper planBenefitMapper = mock(MembershipPlanBenefitMapper.class);

            MembershipBenefitEntity benefit = new MembershipBenefitEntity();
            benefit.setBenefitCode(MembershipEntitlementService.HEALTH_TREE_HOLE_BENEFIT_CODE);
            benefit.setUnitType("TRIAL_7D");
            benefit.setStatus("ACTIVE");
            when(benefitMapper.selectOne(any())).thenReturn(benefit);

            MembershipPlanBenefitEntity config = new MembershipPlanBenefitEntity();
            config.setPlanId(2L);
            config.setBenefitCode(MembershipEntitlementService.HEALTH_TREE_HOLE_BENEFIT_CODE);
            config.setEnabled(true);
            when(planBenefitMapper.selectOne(any())).thenReturn(config);
            when(usageMapper.selectOne(any())).thenReturn(firstUse);
            when(usageMapper.selectCount(any())).thenReturn(0L);

            plan = new MembershipPlanEntity();
            plan.setId(2L);
            plan.setPlanCode("FREE_CUSTOMER");
            membership = new CustomerMembershipEntity();
            membership.setId(3L);
            membership.setTenantId(4L);
            membership.setCustomerId(5L);
            membership.setPlanId(2L);
            service =
                    new MembershipEntitlementService(
                            properties,
                            planMapper,
                            membershipMapper,
                            benefitMapper,
                            planBenefitMapper,
                            usageMapper);
        }

        private MembershipEntitlementService.MembershipSnapshot snapshot(boolean paidActive) {
            MembershipPlanEntity currentPlan = new MembershipPlanEntity();
            currentPlan.setId(plan.getId());
            currentPlan.setPlanCode(paidActive ? "AI_HEALTH_YEARLY" : plan.getPlanCode());
            return new MembershipEntitlementService.MembershipSnapshot(
                    membership, currentPlan, paidActive, LocalDateTime.now(), List.of());
        }
    }
}
