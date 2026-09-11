package com.rayk.health.goldbean.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldRegionEntity;
import com.rayk.health.goldbean.entity.GoldRegionProfitDistributionEntity;
import com.rayk.health.goldbean.entity.GoldRegionProfitEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldRegionMapper;
import com.rayk.health.goldbean.mapper.GoldRegionProfitDistributionMapper;
import com.rayk.health.goldbean.mapper.GoldRegionProfitMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GoldRegionProfitServiceTest {
    private final GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
    private final GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
    private final GoldRegionProfitMapper profitMapper = mock(GoldRegionProfitMapper.class);
    private final GoldRegionProfitDistributionMapper distributionMapper = mock(GoldRegionProfitDistributionMapper.class);
    private final GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);

    @Test
    void automaticallyPaysDirectUplineFromRegionOwnersReferralRegistrationReward() {
        GoldBeanAccountEntity owner = account(3L);
        GoldBeanAccountEntity directUpline = account(2L);
        GoldBeanAccountEntity agent = account(4L);
        agent.setReferrerId(owner.getUserId());
        GoldRegionEntity region = region(30L, owner.getUserId());
        when(accountMapper.selectOne(any())).thenReturn(agent, owner);
        when(regionMapper.selectOne(any())).thenReturn(region);
        when(profitMapper.selectOne(any())).thenReturn(null);
        when(goldBeanService.loadRegionAncestors(owner, true)).thenReturn(List.of(directUpline));

        service().distributeForReward(owner, BigDecimal.valueOf(1_000), "REFERRAL_DIRECT", agent.getUserId(), "REFERRAL:DIRECT:4:2026-09-04");

        ArgumentCaptor<GoldBeanAccountEntity> accountCaptor = ArgumentCaptor.forClass(GoldBeanAccountEntity.class);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(goldBeanService, times(1)).creditRegionProfit(
                accountCaptor.capture(), amountCaptor.capture(), eq(agent.getUserId()), any(), any());
        assertThat(accountCaptor.getAllValues()).containsExactly(directUpline);
        assertThat(amountCaptor.getValue()).isEqualByComparingTo(BigDecimal.valueOf(200));

        ArgumentCaptor<GoldRegionProfitDistributionEntity> distributionCaptor =
                ArgumentCaptor.forClass(GoldRegionProfitDistributionEntity.class);
        verify(distributionMapper, times(1)).insert(distributionCaptor.capture());
        assertThat(distributionCaptor.getAllValues())
                .extracting(GoldRegionProfitDistributionEntity::getRecipientType)
                .containsExactly("DIRECT_UPLINE");
        assertThat(distributionCaptor.getAllValues())
                .extracting(GoldRegionProfitDistributionEntity::getRatePercent)
                .containsExactly(20);
        assertThat(distributionCaptor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
    }

    @Test
    void automaticallyPaysOnlyDirectUplineTwentyPercentFromRegionOwnersReferralRegistrationReward() {
        GoldBeanAccountEntity owner = account(3L);
        GoldBeanAccountEntity directUpline = account(2L);
        GoldBeanAccountEntity grandUpline = account(1L);
        GoldBeanAccountEntity agent = account(4L);
        agent.setReferrerId(owner.getUserId());
        GoldRegionEntity region = region(31L, owner.getUserId());
        when(accountMapper.selectOne(any())).thenReturn(agent, owner);
        when(regionMapper.selectOne(any())).thenReturn(region);
        when(profitMapper.selectOne(any())).thenReturn(null);
        when(goldBeanService.loadRegionAncestors(owner, true))
                .thenReturn(List.of(directUpline, grandUpline));

        service().distributeForReward(owner, BigDecimal.valueOf(1_000), "REFERRAL_DOWNLINE", agent.getUserId(), "REFERRAL:DOWNLINE:4:2026-09-04");

        ArgumentCaptor<GoldBeanAccountEntity> accountCaptor = ArgumentCaptor.forClass(GoldBeanAccountEntity.class);
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(goldBeanService, times(1)).creditRegionProfit(
                accountCaptor.capture(), amountCaptor.capture(), eq(agent.getUserId()), any(), any());
        assertThat(accountCaptor.getAllValues()).containsExactly(directUpline);
        assertThat(amountCaptor.getAllValues()).hasSize(1);
        assertThat(amountCaptor.getValue()).isEqualByComparingTo(BigDecimal.valueOf(200));

        ArgumentCaptor<GoldRegionProfitDistributionEntity> distributionCaptor =
                ArgumentCaptor.forClass(GoldRegionProfitDistributionEntity.class);
        verify(distributionMapper, times(1)).insert(distributionCaptor.capture());
        assertThat(distributionCaptor.getValue().getRecipientType()).isEqualTo("DIRECT_UPLINE");
        assertThat(distributionCaptor.getValue().getRatePercent()).isEqualTo(20);
    }

    @Test
    void ignoresReferralRegistrationRewardWhenRegistrantIsInAnotherCity() {
        GoldBeanAccountEntity owner = account(3L);
        GoldBeanAccountEntity directUpline = account(2L);
        GoldBeanAccountEntity agent = account(4L);
        agent.setReferrerId(owner.getUserId());
        agent.setCity("河南省 / 郑州市");
        GoldRegionEntity region = region(35L, owner.getUserId());
        when(accountMapper.selectOne(any())).thenReturn(agent, owner);
        when(regionMapper.selectOne(any())).thenReturn(region);
        when(goldBeanService.loadRegionAncestors(owner, true)).thenReturn(List.of(directUpline));

        service().distributeForReward(
                owner, BigDecimal.valueOf(1_000), "REFERRAL_DIRECT", agent.getUserId(), "REFERRAL:DIRECT:35:2026-09-04");

        verify(profitMapper, never()).selectOne(any());
        verify(profitMapper, never()).insert(any(GoldRegionProfitEntity.class));
        verify(goldBeanService, never()).creditRegionProfit(any(), any(), any(), any(), any());
        verify(distributionMapper, never()).insert(any(GoldRegionProfitDistributionEntity.class));
    }

    @Test
    void ignoresReferralRewardsPaidToOtherAccountsInTheSameRegion() {
        GoldBeanAccountEntity owner = account(3L);
        GoldBeanAccountEntity directUpline = account(2L);
        directUpline.setReferrerId(owner.getUserId());
        GoldRegionEntity region = region(34L, owner.getUserId());
        when(accountMapper.selectOne(any())).thenReturn(owner);
        when(regionMapper.selectOne(any())).thenReturn(region);

        service().distributeForReward(
                directUpline, BigDecimal.valueOf(1_000), "REFERRAL_DOWNLINE", null, "REFERRAL:DOWNLINE:34:2");

        verify(profitMapper, never()).selectOne(any());
        verify(profitMapper, never()).insert(any(GoldRegionProfitEntity.class));
        verify(goldBeanService, never()).creditRegionProfit(any(), any(), any(), any(), any());
        verify(distributionMapper, never()).insert(any(GoldRegionProfitDistributionEntity.class));
    }

    @Test
    void ignoresNonReferralRewardsForRegionProfit() {
        GoldBeanAccountEntity agent = account(4L);

        service().distributeForReward(agent, BigDecimal.valueOf(1_000), "INITIAL_GRANT", null, "INITIAL:4");
        service().distributeForReward(agent, BigDecimal.valueOf(1_000), "DAILY_REWARD", null, "DAILY:4:2026-09-04");
        service().distributeForReward(agent, BigDecimal.valueOf(1_000), "LEVEL_REWARD", null, "LEVEL:4:DIAMOND");

        verify(accountMapper, never()).selectOne(any());
        verify(regionMapper, never()).selectOne(any());
        verify(profitMapper, never()).selectOne(any());
        verify(profitMapper, never()).insert(any(GoldRegionProfitEntity.class));
        verify(goldBeanService, never()).creditRegionProfit(any(), any(), any(), any(), any());
        verify(distributionMapper, never()).insert(any(GoldRegionProfitDistributionEntity.class));
    }

    @Test
    void doesNotCreateRegionalRewardForTheSameSourceTwice() {
        GoldBeanAccountEntity owner = account(3L);
        GoldBeanAccountEntity agent = account(4L);
        agent.setReferrerId(owner.getUserId());
        GoldRegionEntity region = region(32L, owner.getUserId());
        GoldRegionProfitEntity existing = new GoldRegionProfitEntity();
        existing.setId(99L);
        when(accountMapper.selectOne(any())).thenReturn(agent, owner);
        when(regionMapper.selectOne(any())).thenReturn(region);
        when(profitMapper.selectOne(any())).thenReturn(existing);

        service().distributeForReward(owner, BigDecimal.valueOf(60), "REFERRAL_DIRECT", agent.getUserId(), "REFERRAL:DIRECT:4:2026-09-04");

        verify(goldBeanService, never()).creditRegionProfit(any(), any(), any(), any(), any());
        verify(distributionMapper, never()).insert(any(GoldRegionProfitDistributionEntity.class));
    }

    @Test
    void ignoresARewardWhenTheNearestRegionIsBeyondTheSupportedChain() {
        GoldBeanAccountEntity owner = account(3L);
        GoldBeanAccountEntity agent = account(4L);
        agent.setReferrerId(owner.getUserId());
        GoldRegionEntity region = region(33L, owner.getUserId());
        when(accountMapper.selectOne(any())).thenReturn(agent, owner);
        when(regionMapper.selectOne(any())).thenReturn(region);
        when(goldBeanService.loadRegionAncestors(owner, true))
                .thenReturn(List.of(account(2L), account(1L), account(0L)));

        service().distributeForReward(owner, BigDecimal.ONE, "REFERRAL_DOWNLINE", agent.getUserId(), "REFERRAL:DOWNLINE:4:2026-09-04");

        verify(profitMapper, never()).insert(any(GoldRegionProfitEntity.class));
        verify(goldBeanService, never()).creditRegionProfit(any(), any(), any(), any(), any());
    }

    private GoldRegionProfitService service() {
        return new GoldRegionProfitService(
                new GoldBeanProperties(true, true, 60, 60, 20, 100, 99_800, 7, 50,
                        true, "normal_member_998", "gold_bean", 100, 10_000,
                        "机器人权益服务群", ""),
                accountMapper,
                regionMapper,
                profitMapper,
                distributionMapper,
                goldBeanService,
                mock(com.rayk.health.system.mapper.SysUserMapper.class),
                mock(com.rayk.health.system.mapper.SysTenantMapper.class));
    }

    private static GoldBeanAccountEntity account(long userId) {
        GoldBeanAccountEntity account = new GoldBeanAccountEntity();
        account.setTenantId(2L);
        account.setUserId(userId);
        account.setRegistrationFeeStatus("PAID");
        account.setStatus("ACTIVE");
        account.setMemberLevel("DIAMOND");
        account.setCity("河南省 / 洛阳市");
        account.setDigitalBankBalance(BigDecimal.ZERO);
        account.setTradingBalance(BigDecimal.ZERO);
        account.setDeleted(0);
        account.setVersion(0);
        return account;
    }

    private static GoldRegionEntity region(long regionId, long ownerUserId) {
        GoldRegionEntity region = new GoldRegionEntity();
        region.setId(regionId);
        region.setTenantId(2L);
        region.setOwnerUserId(ownerUserId);
        region.setCity("河南省 / 洛阳市");
        region.setDepth(3);
        region.setStatus("ACTIVE");
        region.setDeleted(0);
        region.setVersion(0);
        return region;
    }
}
