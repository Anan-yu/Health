package com.rayk.health.goldbean.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanReferralEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanReferralMapper;
import com.rayk.health.goldbean.vo.GoldBeanReferralNetworkVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.system.entity.SysUserEntity;
import com.rayk.health.system.mapper.SysUserMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class GoldBeanReferralNetworkServiceTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCurrentCustomersTwoLevelNetworkAndSevenDayTrend() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanReferralMapper referralMapper = mock(GoldBeanReferralMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        GoldBeanReferralNetworkService service =
                new GoldBeanReferralNetworkService(accountMapper, referralMapper, userMapper);

        GoldBeanAccountEntity self = account(1L, "COPPER", 2);
        GoldBeanAccountEntity directA = account(2L, "ORDINARY", 1);
        GoldBeanAccountEntity directB = account(3L, "ORDINARY", 0);
        GoldBeanAccountEntity secondLevel = account(4L, "ORDINARY", 0);
        when(accountMapper.selectOne(any())).thenReturn(self);
        when(accountMapper.selectList(any())).thenReturn(List.of(self, directA, directB, secondLevel));

        LocalDateTime now = LocalDateTime.now();
        GoldBeanReferralEntity first = relation(1L, 2L, now.minusDays(3));
        GoldBeanReferralEntity second = relation(1L, 3L, now.minusDays(1));
        GoldBeanReferralEntity child = relation(2L, 4L, now.minusHours(6));
        when(referralMapper.selectList(any())).thenReturn(List.of(second, first), List.of(child));
        when(userMapper.selectList(any())).thenReturn(List.of(
                user(1L, "我"),
                user(2L, "小羊"),
                user(3L, "小健"),
                user(4L, "小康")));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal(
                        "jti",
                        "customer",
                        1L,
                        9L,
                        List.of("CUSTOMER"),
                        List.of("self:health-record"),
                        "CUSTOMER"),
                null));

        GoldBeanReferralNetworkVo result = service.network();

        assertThat(result.selfDisplayName()).isEqualTo("我");
        assertThat(result.directReferralCount()).isEqualTo(2);
        assertThat(result.teamMemberCount()).isEqualTo(3);
        assertThat(result.branches()).hasSize(2);
        assertThat(result.branches().get(1).members())
                .extracting("displayName")
                .containsExactly("小康");
        assertThat(result.trend()).hasSize(7);
        assertThat(result.trend().getLast().directReferralCount()).isEqualTo(2);
        assertThat(result.trend().getLast().levelName()).isEqualTo("铜牌会员");
    }

    private GoldBeanAccountEntity account(long userId, String level, int directCount) {
        GoldBeanAccountEntity account = new GoldBeanAccountEntity();
        account.setUserId(userId);
        account.setTenantId(9L);
        account.setMemberLevel(level);
        account.setDirectReferralCount(directCount);
        return account;
    }

    private GoldBeanReferralEntity relation(long referrerId, long referredId, LocalDateTime registeredAt) {
        GoldBeanReferralEntity relation = new GoldBeanReferralEntity();
        relation.setId(referredId * 10);
        relation.setTenantId(9L);
        relation.setReferrerId(referrerId);
        relation.setReferredId(referredId);
        relation.setStatus("ACTIVE");
        relation.setRegisteredAt(registeredAt);
        return relation;
    }

    private SysUserEntity user(long userId, String displayName) {
        SysUserEntity user = new SysUserEntity();
        user.setId(userId);
        user.setTenantId(9L);
        user.setDisplayName(displayName);
        return user;
    }
}
