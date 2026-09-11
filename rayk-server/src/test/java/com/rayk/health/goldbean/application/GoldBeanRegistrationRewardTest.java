package com.rayk.health.goldbean.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanLedgerEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanLedgerMapper;
import com.rayk.health.goldbean.mapper.GoldBeanOrderMapper;
import com.rayk.health.goldbean.mapper.GoldBeanReferralMapper;
import com.rayk.health.goldbean.mapper.GoldRegionMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.rayk.health.goldbean.vo.GoldBeanSummaryVo;
import com.rayk.health.security.service.CurrentPrincipal;

class GoldBeanRegistrationRewardTest {
    @Test
    void countsTheRegistrationDateAsTheFirstDailyRewardDay() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanReferralMapper referralMapper = mock(GoldBeanReferralMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldBeanOrderMapper orderMapper = mock(GoldBeanOrderMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanPlatformInviteService platformInviteService = mock(GoldBeanPlatformInviteService.class);
        GoldBeanApplicationService service = new GoldBeanApplicationService(
                new GoldBeanProperties(true, true, 60, 60, 20, 100, 99_800, 7, 50,
                        true, "normal_member_998", "gold_bean", 100, 10_000,
                        "机器人权益服务群", ""),
                accountMapper,
                referralMapper,
                ledgerMapper,
                orderMapper,
                regionMapper,
                platformInviteService);

        GoldBeanAccountEntity account = unpaidAccount();
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(ledgerMapper.selectOne(any())).thenReturn(null);

        service.completePlatformRegistration(2L, 3L, "", "", "ORDER-1");

        assertThat(account.getRegistrationFeeStatus()).isEqualTo("PAID");
        assertThat(account.getReferralCode()).isNotBlank();
        assertThat(account.getDailyRewardDays()).isEqualTo(1);
        assertThat(account.getDailyRewardLastAt().toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(account.getDigitalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(account.getTradingBalance()).isEqualByComparingTo(BigDecimal.valueOf(30));

        ArgumentCaptor<GoldBeanLedgerEntity> ledgerCaptor = ArgumentCaptor.forClass(GoldBeanLedgerEntity.class);
        org.mockito.Mockito.verify(ledgerMapper, org.mockito.Mockito.times(2)).insert(ledgerCaptor.capture());
        List<GoldBeanLedgerEntity> ledgers = ledgerCaptor.getAllValues();
        assertThat(ledgers).allMatch(item -> "DAILY_REWARD".equals(item.getEventType()));
        assertThat(ledgers).allMatch(item -> item.getDescription().contains("第1天"));
    }

    @Test
    void referralRegistrationRewardsDirectAndAllDownlineAncestors() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanReferralMapper referralMapper = mock(GoldBeanReferralMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldBeanOrderMapper orderMapper = mock(GoldBeanOrderMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanPlatformInviteService platformInviteService = mock(GoldBeanPlatformInviteService.class);
        GoldBeanApplicationService service = new GoldBeanApplicationService(
                new GoldBeanProperties(true, true, 60, 60, 20, 100, 99_800, 7, 50,
                        true, "normal_member_998", "gold_bean", 100, 10_000,
                        "机器人权益服务群", ""),
                accountMapper,
                referralMapper,
                ledgerMapper,
                orderMapper,
                regionMapper,
                platformInviteService);

        GoldBeanAccountEntity referred = unpaidAccount();
        referred.setUserId(40L);
        GoldBeanAccountEntity referrer = paidAccount(30L);
        referrer.setReferrerId(20L);
        GoldBeanAccountEntity secondLevelReferrer = paidAccount(20L);
        secondLevelReferrer.setReferrerId(10L);
        GoldBeanAccountEntity rootReferrer = paidAccount(10L);
        when(accountMapper.selectOne(any())).thenReturn(referred, referrer, secondLevelReferrer, rootReferrer);
        when(ledgerMapper.selectOne(any())).thenReturn(null);

        service.completeRegistration(2L, 40L, "REFERRER-CODE", 30L, "", "REFERRER", "ORDER-2");

        ArgumentCaptor<GoldBeanLedgerEntity> ledgerCaptor = ArgumentCaptor.forClass(GoldBeanLedgerEntity.class);
        org.mockito.Mockito.verify(ledgerMapper, org.mockito.Mockito.times(8)).insert(ledgerCaptor.capture());
        List<GoldBeanLedgerEntity> ledgers = ledgerCaptor.getAllValues();
        assertThat(ledgers).filteredOn(item -> "REFERRAL_DIRECT".equals(item.getEventType())).hasSize(2);
        assertThat(ledgers).filteredOn(item -> "REFERRAL_DOWNLINE".equals(item.getEventType())).hasSize(4);
        assertThat(ledgers).filteredOn(item -> "REFERRAL_DIRECT".equals(item.getEventType()))
                .extracting(GoldBeanLedgerEntity::getAmount)
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo(BigDecimal.valueOf(5.5)));
        assertThat(ledgers).filteredOn(item -> "REFERRAL_DOWNLINE".equals(item.getEventType()))
                .extracting(GoldBeanLedgerEntity::getAmount)
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo(BigDecimal.valueOf(2.5)));
        assertThat(referrer.getDigitalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(5.5));
        assertThat(referrer.getTradingBalance()).isEqualByComparingTo(BigDecimal.valueOf(5.5));
        assertThat(secondLevelReferrer.getDigitalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(2.5));
        assertThat(secondLevelReferrer.getTradingBalance()).isEqualByComparingTo(BigDecimal.valueOf(2.5));
        assertThat(rootReferrer.getDigitalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(2.5));
        assertThat(rootReferrer.getTradingBalance()).isEqualByComparingTo(BigDecimal.valueOf(2.5));
        org.mockito.Mockito.verify(accountMapper, org.mockito.Mockito.times(4)).selectOne(any());
    }

    @Test
    void successfulDirectReferralRestoresOneLevelDuringDemotionAndStartsProtection() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanReferralMapper referralMapper = mock(GoldBeanReferralMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldBeanOrderMapper orderMapper = mock(GoldBeanOrderMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanPlatformInviteService platformInviteService = mock(GoldBeanPlatformInviteService.class);
        GoldBeanApplicationService service = new GoldBeanApplicationService(
                new GoldBeanProperties(true, true, 60, 60, 20, 100, 99_800, 7, 50,
                        true, "normal_member_998", "gold_bean", 100, 10_000,
                        "机器人权益服务群", ""),
                accountMapper,
                referralMapper,
                ledgerMapper,
                orderMapper,
                regionMapper,
                platformInviteService);

        GoldBeanAccountEntity referred = unpaidAccount();
        referred.setUserId(40L);
        GoldBeanAccountEntity referrer = paidAccount(30L);
        referrer.setMemberLevel("SILVER");
        referrer.setHistoricalLevel("DIAMOND");
        referrer.setDirectReferralCount(9);
        referrer.setLastLevelDropAt(LocalDateTime.now());
        referrer.setTradeLimitPercent(50);
        when(accountMapper.selectOne(any())).thenReturn(referred, referrer);
        when(ledgerMapper.selectOne(any())).thenReturn(null);

        service.completeRegistration(2L, 40L, "REFERRER-CODE", 30L, "", "REFERRER", "ORDER-RESTORE");

        assertThat(referrer.getDirectReferralCount()).isEqualTo(10);
        assertThat(referrer.getMemberLevel()).isEqualTo("GOLD");
        assertThat(referrer.getHistoricalLevel()).isEqualTo("DIAMOND");
        assertThat(referrer.getProtectionUntil()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(referrer.getLastLevelDropAt()).isNull();
        assertThat(referrer.getTradeLimitPercent()).isEqualTo(100);
    }

    @Test
    void referralAtHistoricalHighestOnlyRefreshesProtection() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanReferralMapper referralMapper = mock(GoldBeanReferralMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldBeanOrderMapper orderMapper = mock(GoldBeanOrderMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanPlatformInviteService platformInviteService = mock(GoldBeanPlatformInviteService.class);
        GoldBeanApplicationService service = new GoldBeanApplicationService(
                new GoldBeanProperties(true, true, 60, 60, 20, 100, 99_800, 7, 50,
                        true, "normal_member_998", "gold_bean", 100, 10_000,
                        "机器人权益服务群", ""),
                accountMapper,
                referralMapper,
                ledgerMapper,
                orderMapper,
                regionMapper,
                platformInviteService);

        GoldBeanAccountEntity referred = unpaidAccount();
        referred.setUserId(41L);
        GoldBeanAccountEntity referrer = paidAccount(31L);
        referrer.setMemberLevel("DIAMOND");
        referrer.setHistoricalLevel("DIAMOND");
        referrer.setDirectReferralCount(50);
        referrer.setProtectionUntil(LocalDateTime.now().plusDays(1));
        when(accountMapper.selectOne(any())).thenReturn(referred, referrer);
        when(ledgerMapper.selectOne(any())).thenReturn(null);

        service.completeRegistration(2L, 41L, "REFERRER-CODE", 31L, "", "REFERRER", "ORDER-TOP");

        assertThat(referrer.getDirectReferralCount()).isEqualTo(51);
        assertThat(referrer.getMemberLevel()).isEqualTo("DIAMOND");
        assertThat(referrer.getHistoricalLevel()).isEqualTo("DIAMOND");
        assertThat(referrer.getProtectionUntil()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(referrer.getLastLevelDropAt()).isNull();
    }

    @Test
    void referralDuringDailyRewardSuppressesReminderButDefersProtection() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanReferralMapper referralMapper = mock(GoldBeanReferralMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldBeanOrderMapper orderMapper = mock(GoldBeanOrderMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanPlatformInviteService platformInviteService = mock(GoldBeanPlatformInviteService.class);
        GoldBeanApplicationService service = new GoldBeanApplicationService(
                new GoldBeanProperties(true, true, 60, 60, 20, 100, 99_800, 7, 50,
                        true, "normal_member_998", "gold_bean", 100, 10_000,
                        "机器人权益服务群", ""),
                accountMapper,
                referralMapper,
                ledgerMapper,
                orderMapper,
                regionMapper,
                platformInviteService);

        LocalDateTime now = LocalDateTime.now();
        GoldBeanAccountEntity referred = unpaidAccount();
        referred.setUserId(42L);
        GoldBeanAccountEntity referrer = paidAccount(31L);
        referrer.setDailyRewardStartAt(now.minusDays(14));
        referrer.setDailyRewardLastAt(now.minusDays(1));
        referrer.setDailyRewardDays(15);
        referrer.setProtectionStartedAt(null);
        referrer.setProtectionUntil(null);
        referrer.setLastProtectionReferralAt(null);
        referrer.setDirectReferralCount(0);
        when(accountMapper.selectOne(any())).thenReturn(referred, referrer, referrer);
        when(ledgerMapper.selectOne(any())).thenReturn(null);

        service.completeRegistration(2L, 42L, "REFERRER-CODE", 31L, "", "REFERRER", "ORDER-DEFER");

        assertThat(referrer.getDirectReferralCount()).isEqualTo(1);
        assertThat(referrer.getProtectionStartedAt()).isNull();
        assertThat(referrer.getProtectionUntil()).isNull();
        assertThat(referrer.getTradeLimitPercent()).isEqualTo(100);

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 31L, 2L, List.of(), List.of(), "CUSTOMER"), null));
        try {
            GoldBeanSummaryVo summary = service.summary();
            assertThat(summary.dailyRewardDays()).isEqualTo(16);
            assertThat(summary.reminderText()).isNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void levelUnlockDuringDailyRewardDefersProtectionUntilDayTwentyCompletes() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanReferralMapper referralMapper = mock(GoldBeanReferralMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldBeanOrderMapper orderMapper = mock(GoldBeanOrderMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanPlatformInviteService platformInviteService = mock(GoldBeanPlatformInviteService.class);
        GoldBeanApplicationService service = new GoldBeanApplicationService(
                new GoldBeanProperties(true, true, 60, 60, 20, 100, 99_800, 7, 50,
                        true, "normal_member_998", "gold_bean", 100, 10_000,
                        "机器人权益服务群", ""),
                accountMapper,
                referralMapper,
                ledgerMapper,
                orderMapper,
                regionMapper,
                platformInviteService);

        LocalDateTime now = LocalDateTime.now();
        GoldBeanAccountEntity referred = unpaidAccount();
        referred.setUserId(43L);
        GoldBeanAccountEntity referrer = paidAccount(32L);
        referrer.setMemberLevel("COPPER");
        referrer.setHistoricalLevel("COPPER");
        referrer.setDirectReferralCount(8);
        referrer.setDailyRewardStartAt(now.minusDays(9));
        referrer.setDailyRewardLastAt(now.minusDays(1));
        referrer.setDailyRewardDays(10);
        referrer.setProtectionStartedAt(null);
        referrer.setProtectionUntil(null);
        referrer.setLastProtectionReferralAt(null);
        when(accountMapper.selectOne(any())).thenReturn(referred, referrer, referrer);
        when(ledgerMapper.selectOne(any())).thenReturn(null);

        service.completeRegistration(2L, 43L, "REFERRER-CODE", 32L, "", "REFERRER", "ORDER-DEFER-LEVEL");

        assertThat(referrer.getDirectReferralCount()).isEqualTo(9);
        assertThat(referrer.getMemberLevel()).isEqualTo("SILVER");
        assertThat(referrer.getHistoricalLevel()).isEqualTo("SILVER");
        assertThat(referrer.getProtectionStartedAt()).isNull();
        assertThat(referrer.getProtectionUntil()).isNull();
        assertThat(referrer.getLastProtectionReferralAt()).isNull();

        referrer.setDailyRewardStartAt(now.minusDays(19));
        referrer.setDailyRewardLastAt(now.minusDays(1));
        referrer.setDailyRewardDays(19);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 32L, 2L, List.of(), List.of(), "CUSTOMER"), null));
        try {
            GoldBeanSummaryVo summary = service.summary();

            assertThat(summary.dailyRewardDays()).isEqualTo(20);
            assertThat(referrer.getMemberLevel()).isEqualTo("SILVER");
            assertThat(referrer.getProtectionStartedAt()).isNotNull();
            assertThat(referrer.getProtectionUntil()).isAfter(now.plusDays(6));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private GoldBeanAccountEntity unpaidAccount() {
        GoldBeanAccountEntity account = new GoldBeanAccountEntity();
        account.setTenantId(2L);
        account.setUserId(3L);
        account.setMemberLevel("ordinary");
        account.setHistoricalLevel("ordinary");
        account.setDirectReferralCount(0);
        account.setRegistrationFeeStatus("UNPAID");
        account.setDigitalBankBalance(BigDecimal.ZERO);
        account.setTradingBalance(BigDecimal.ZERO);
        account.setDailyRewardStartAt(LocalDateTime.now());
        account.setDailyRewardDays(0);
        account.setTradeLimitPercent(100);
        account.setStatus("ACTIVE");
        account.setDeleted(0);
        account.setVersion(0);
        return account;
    }

    private GoldBeanAccountEntity paidAccount(long userId) {
        GoldBeanAccountEntity account = unpaidAccount();
        account.setUserId(userId);
        account.setReferralCode("REFERRER-CODE");
        account.setRegistrationFeeStatus("PAID");
        account.setRegistrationFeeRecipient("PLATFORM");
        account.setDailyRewardDays(20);
        account.setDailyRewardLastAt(LocalDateTime.now());
        return account;
    }
}
