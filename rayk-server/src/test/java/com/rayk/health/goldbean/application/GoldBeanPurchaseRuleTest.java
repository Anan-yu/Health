package com.rayk.health.goldbean.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanLedgerEntity;
import com.rayk.health.goldbean.entity.GoldRegionEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanLedgerMapper;
import com.rayk.health.goldbean.mapper.GoldBeanOrderMapper;
import com.rayk.health.goldbean.mapper.GoldBeanReferralMapper;
import com.rayk.health.goldbean.mapper.GoldRegionMapper;
import com.rayk.health.goldbean.vo.GoldBeanSummaryVo;
import com.rayk.health.security.service.CurrentPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class GoldBeanPurchaseRuleTest {
    @Test
    void splitsPlatformPurchaseBetweenDigitalBankAndTrading() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        account.setMemberLevel("DIAMOND");
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(ledgerMapper.selectOne(any())).thenReturn(null);
        GoldBeanApplicationService service = service(accountMapper, ledgerMapper);

        service.completePlatformPurchase(2L, 3L, 10L, "GBP-1");

        assertThat(account.getDigitalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(account.getTradingBalance()).isEqualByComparingTo(BigDecimal.valueOf(5));
        ArgumentCaptor<GoldBeanLedgerEntity> ledgerCaptor = ArgumentCaptor.forClass(GoldBeanLedgerEntity.class);
        verify(ledgerMapper, org.mockito.Mockito.times(2)).insert(ledgerCaptor.capture());
        List<GoldBeanLedgerEntity> ledgers = ledgerCaptor.getAllValues();
        assertThat(ledgers).extracting(GoldBeanLedgerEntity::getBucket)
                .containsExactlyInAnyOrder("DIGITAL_BANK", "TRADING");
        assertThat(ledgers).allMatch(item -> item.getAmount().compareTo(BigDecimal.valueOf(5)) == 0);
        assertThat(ledgers).allMatch(item -> "PLATFORM_PURCHASE".equals(item.getEventType()));
        assertThat(ledgers).allMatch(item -> item.getDescription().contains("双账本尽量各一半"));
    }

    @Test
    void refusesPlatformPurchaseForMemberRegisteredThroughReferrer() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        when(accountMapper.selectOne(any())).thenReturn(account);
        GoldBeanApplicationService service = service(accountMapper, ledgerMapper);

        assertThatThrownBy(() -> service.completePlatformPurchase(2L, 3L, 10L, "GBP-2"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.GOLD_BEAN_PLATFORM_PURCHASE_NOT_ELIGIBLE));

        assertThat(account.getDigitalBankBalance()).isZero();
        assertThat(account.getTradingBalance()).isZero();
        verifyNoInteractions(ledgerMapper);
    }

    @Test
    void roundsOddPurchaseQuantityToDigitalBank() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldBeanAccountEntity account = account("PLATFORM");
        account.setMemberLevel("DIAMOND");
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(ledgerMapper.selectOne(any())).thenReturn(null);
        GoldBeanApplicationService service = service(accountMapper, ledgerMapper);

        service.completePlatformPurchase(2L, 3L, 9L, "GBP-3");

        assertThat(account.getDigitalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
        assertThat(account.getTradingBalance()).isEqualByComparingTo(BigDecimal.valueOf(4.5));
        ArgumentCaptor<GoldBeanLedgerEntity> ledgerCaptor = ArgumentCaptor.forClass(GoldBeanLedgerEntity.class);
        verify(ledgerMapper, org.mockito.Mockito.times(2)).insert(ledgerCaptor.capture());
        List<GoldBeanLedgerEntity> ledgers = ledgerCaptor.getAllValues();
        assertThat(ledgers).extracting(GoldBeanLedgerEntity::getBucket)
                .containsExactlyInAnyOrder("DIGITAL_BANK", "TRADING");
        assertThat(ledgers).extracting(GoldBeanLedgerEntity::getAmount)
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo(BigDecimal.valueOf(4.5)));
        assertThat(ledgers).allMatch(item -> "PLATFORM_PURCHASE".equals(item.getEventType()));
    }

    @Test
    void legendaryPurchaseCreditsOnlyDigitalBank() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(ledgerMapper.selectOne(any())).thenReturn(null);
        GoldBeanApplicationService service = service(accountMapper, ledgerMapper);

        service.completeLegendaryBankPurchase(2L, 3L, 17L, "GBP-LEGENDARY-1");

        assertThat(account.getDigitalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(17));
        assertThat(account.getTradingBalance()).isZero();
        ArgumentCaptor<GoldBeanLedgerEntity> ledgerCaptor = ArgumentCaptor.forClass(GoldBeanLedgerEntity.class);
        verify(ledgerMapper).insert(ledgerCaptor.capture());
        GoldBeanLedgerEntity ledger = ledgerCaptor.getValue();
        assertThat(ledger.getBucket()).isEqualTo("DIGITAL_BANK");
        assertThat(ledger.getEventType()).isEqualTo("LEGENDARY_BANK_PURCHASE");
        assertThat(ledger.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(17));
    }

    @Test
    void refusesPlatformPurchaseForNonDiamondMember() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        account.setMemberLevel("GOLD");
        when(accountMapper.selectOne(any())).thenReturn(account);
        GoldBeanApplicationService service = service(accountMapper, ledgerMapper);

        assertThatThrownBy(() -> service.completePlatformPurchase(2L, 3L, 10L, "GBP-4"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.GOLD_BEAN_PLATFORM_PURCHASE_NOT_ELIGIBLE));
        verifyNoInteractions(ledgerMapper);
    }

    @Test
    void platformAgentOrDiamondMemberCanPurchaseFromPlatform() {
        GoldBeanApplicationService service = service(mock(GoldBeanAccountMapper.class), mock(GoldBeanLedgerMapper.class));
        GoldBeanAccountEntity diamondReferredMember = account("REFERRER");
        diamondReferredMember.setMemberLevel("DIAMOND");
        GoldBeanAccountEntity ordinaryPlatformMember = account("PLATFORM");

        assertThat(service.platformPurchaseEligible(diamondReferredMember)).isTrue();
        assertThat(service.platformPurchaseEligible(ordinaryPlatformMember)).isTrue();
    }

    @Test
    void platformAgentKeepsPurchaseEligibilityAfterDiamondLevelDrop() {
        GoldBeanApplicationService service = service(mock(GoldBeanAccountMapper.class), mock(GoldBeanLedgerMapper.class));
        GoldBeanAccountEntity platformAgent = account("PLATFORM");
        platformAgent.setMemberLevel("GOLD");

        assertThat(service.platformPurchaseEligible(platformAgent)).isTrue();
    }

    @Test
    void exposesPlatformPurchaseForPlatformAgentSummary() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldBeanAccountEntity platformAgent = account("PLATFORM");
        when(accountMapper.selectOne(any())).thenReturn(platformAgent);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 3L, 2L, List.of(), List.of(), "CUSTOMER"), null));
        try {
            GoldBeanSummaryVo summary = service(accountMapper, ledgerMapper).summary();

            assertThat(summary.goldBeanPurchaseEnabled()).isTrue();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void losingDiamondDisablesPlatformPurchaseButKeepsOpenedRegion() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        account.setMemberLevel("DIAMOND");
        account.setProtectionUntil(LocalDateTime.now().minusMinutes(1));
        GoldRegionEntity region = new GoldRegionEntity();
        region.setId(88L);
        region.setCity("上海市");
        region.setDepth(1);
        region.setStatus("ACTIVE");
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(regionMapper.selectOne(any())).thenReturn(region);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 3L, 2L, List.of(), List.of(), "CUSTOMER"), null));
        try {
            GoldBeanSummaryVo summary = service(accountMapper, ledgerMapper, regionMapper).summary();

            assertThat(account.getMemberLevel()).isEqualTo("GOLD");
            assertThat(summary.goldBeanPurchaseEnabled()).isFalse();
            assertThat(summary.regionId()).isEqualTo("88");
            assertThat(summary.regionCity()).isEqualTo("上海市");
            verify(regionMapper, org.mockito.Mockito.never()).delete(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void protectionExpiryStartsDailyDemotionWithFullLimitAboveOrdinary() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        account.setMemberLevel("GOLD");
        account.setDirectReferralCount(3);
        account.setProtectionUntil(LocalDateTime.now().minusMinutes(1));
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(ledgerMapper.selectOne(any())).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 3L, 2L, List.of(), List.of(), "CUSTOMER"), null));
        try {
            service(accountMapper, ledgerMapper, regionMapper).summary();
            assertThat(account.getMemberLevel()).isEqualTo("SILVER");
            assertThat(account.getTradeLimitPercent()).isEqualTo(100);
            assertThat(account.getProtectionUntil()).isNotNull().isBefore(LocalDateTime.now());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void startsDemotionOnTheEighthDayAfterProtection() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        account.setMemberLevel("GOLD");
        account.setHistoricalLevel("DIAMOND");
        account.setProtectionUntil(LocalDateTime.now().minusMinutes(1));
        account.setTradeLimitPercent(100);
        when(accountMapper.selectOne(any())).thenReturn(account);

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 3L, 2L, List.of(), List.of(), "CUSTOMER"), null));
        try {
            GoldBeanSummaryVo summary = service(accountMapper, ledgerMapper, regionMapper).summary();

            assertThat(account.getMemberLevel()).isEqualTo("SILVER");
            assertThat(account.getLastLevelDropAt()).isNotNull();
            assertThat(account.getLastLevelDropAt().toLocalDate()).isEqualTo(java.time.LocalDate.now());
            assertThat(account.getProtectionUntil()).isNotNull().isBefore(LocalDateTime.now());
            assertThat(account.getTradeLimitPercent()).isEqualTo(100);
            assertThat(summary.reminderText()).contains("保护期已结束");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void demotesOnlyOncePerCalendarDayAndLimitsAtOrdinaryMembership() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        account.setMemberLevel("GOLD");
        account.setHistoricalLevel("DIAMOND");
        account.setProtectionUntil(LocalDateTime.now().minusMinutes(1));
        when(accountMapper.selectOne(any())).thenReturn(account);

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 3L, 2L, List.of(), List.of(), "CUSTOMER"), null));
        try {
            GoldBeanApplicationService service = service(accountMapper, ledgerMapper, regionMapper);
            service.summary();
            assertThat(account.getMemberLevel()).isEqualTo("SILVER");
            service.summary();
            assertThat(account.getMemberLevel()).isEqualTo("SILVER");

            // Simulate the next calendar day being settled.
            account.setLastLevelDropAt(LocalDateTime.now().minusDays(1));
            service.summary();
            assertThat(account.getMemberLevel()).isEqualTo("COPPER");
            assertThat(account.getTradeLimitPercent()).isEqualTo(100);
            assertThat(account.getProtectionUntil()).isNotNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void keepsFullTradingOnTheFinalDropAndLimitsOnTheFollowingDay() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        account.setMemberLevel("COPPER");
        account.setHistoricalLevel("DIAMOND");
        account.setProtectionUntil(LocalDateTime.now().minusMinutes(1));
        when(accountMapper.selectOne(any())).thenReturn(account);

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 3L, 2L, List.of(), List.of(), "CUSTOMER"), null));
        try {
            GoldBeanApplicationService service = service(accountMapper, ledgerMapper, regionMapper);
            GoldBeanSummaryVo summary = service.summary();

            assertThat(account.getMemberLevel()).isEqualTo("ORDINARY");
            assertThat(account.getProtectionUntil()).isNull();
            assertThat(account.getTradeLimitPercent()).isEqualTo(100);
            assertThat(summary.tradeLimitPercent()).isEqualTo(100);

            // Once ordinary membership has been reached, the following no-referral day
            // activates the 50% limit even though there is no lower level to enter.
            account.setLastLevelDropAt(LocalDateTime.now().minusDays(1));
            summary = service.summary();
            assertThat(account.getMemberLevel()).isEqualTo("ORDINARY");
            assertThat(account.getTradeLimitPercent()).isEqualTo(50);
            assertThat(summary.tradeLimitPercent()).isEqualTo(50);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void remindsOnProtectionDaysFourThroughSixOnly() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        account.setMemberLevel("GOLD");
        account.setHistoricalLevel("GOLD");
        account.setDirectReferralCount(1);
        account.setTradeLimitPercent(100);
        when(accountMapper.selectOne(any())).thenReturn(account);

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 3L, 2L, List.of(), List.of(), "CUSTOMER"), null));
        try {
            GoldBeanApplicationService service = service(accountMapper, ledgerMapper, regionMapper);
            LocalDateTime now = LocalDateTime.now();

            account.setProtectionStartedAt(now.minusDays(3));
            account.setProtectionUntil(now.plusDays(4));
            assertThat(service.summary().reminderText()).contains("第4天").contains("推荐1位新人");

            account.setProtectionStartedAt(now.minusDays(5));
            account.setProtectionUntil(now.plusDays(2));
            assertThat(service.summary().reminderText()).contains("第6天").contains("刷新7天保护期");

            account.setProtectionStartedAt(now.minusDays(6));
            account.setProtectionUntil(now.plusDays(1));
            assertThat(service.summary().reminderText()).isNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void startsProtectionAfterTheTwentiethDailyReward() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        LocalDateTime now = LocalDateTime.now();
        account.setDailyRewardStartAt(now.minusDays(19));
        account.setDailyRewardLastAt(now.minusDays(1));
        account.setDailyRewardDays(19);
        account.setProtectionStartedAt(null);
        account.setProtectionUntil(null);
        account.setLastProtectionReferralAt(null);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(ledgerMapper.selectOne(any())).thenReturn(null);

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 3L, 2L, List.of(), List.of(), "CUSTOMER"), null));
        try {
            GoldBeanSummaryVo summary = service(accountMapper, ledgerMapper, regionMapper).summary();

            assertThat(account.getDailyRewardDays()).isEqualTo(20);
            assertThat(account.getProtectionStartedAt()).isNotNull().isAfterOrEqualTo(now);
            assertThat(account.getProtectionUntil()).isAfter(now.plusDays(6));
            assertThat(account.getTradeLimitPercent()).isEqualTo(100);
            assertThat(summary.reminderText()).isNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void remindsFromDayFifteenUntilDayNineteenWhenThereIsNoReferral() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        LocalDateTime now = LocalDateTime.now();
        account.setDailyRewardStartAt(now.minusDays(14));
        account.setDailyRewardLastAt(now);
        account.setDailyRewardDays(15);
        account.setProtectionStartedAt(null);
        account.setProtectionUntil(null);
        account.setLastProtectionReferralAt(null);
        when(accountMapper.selectOne(any())).thenReturn(account);

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 3L, 2L, List.of(), List.of(), "CUSTOMER"), null));
        try {
            GoldBeanSummaryVo summary = service(accountMapper, ledgerMapper, regionMapper).summary();

            assertThat(summary.reminderText()).contains("连续1天").contains("第20天前");
            assertThat(account.getProtectionUntil()).isNull();
            assertThat(account.getTradeLimitPercent()).isEqualTo(100);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void continuesDailyDemotionAfterProtectionTimestampsAreCleared() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanLedgerMapper ledgerMapper = mock(GoldBeanLedgerMapper.class);
        GoldRegionMapper regionMapper = mock(GoldRegionMapper.class);
        GoldBeanAccountEntity account = account("REFERRER");
        account.setMemberLevel("GOLD");
        account.setHistoricalLevel("DIAMOND");
        account.setProtectionUntil(null);
        account.setLastProtectionReferralAt(null);
        account.setLastLevelDropAt(LocalDateTime.now().minusDays(1));
        account.setTradeLimitPercent(50);
        when(accountMapper.selectOne(any())).thenReturn(account);

        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 3L, 2L, List.of(), List.of(), "CUSTOMER"), null));
        try {
            service(accountMapper, ledgerMapper, regionMapper).summary();
            assertThat(account.getMemberLevel()).isEqualTo("SILVER");
            assertThat(account.getLastLevelDropAt().toLocalDate()).isEqualTo(java.time.LocalDate.now());
            assertThat(account.getTradeLimitPercent()).isEqualTo(100);

            service(accountMapper, ledgerMapper, regionMapper).summary();
            assertThat(account.getMemberLevel()).isEqualTo("SILVER");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private GoldBeanApplicationService service(
            GoldBeanAccountMapper accountMapper, GoldBeanLedgerMapper ledgerMapper) {
        return service(accountMapper, ledgerMapper, mock(GoldRegionMapper.class));
    }

    private GoldBeanApplicationService service(
            GoldBeanAccountMapper accountMapper,
            GoldBeanLedgerMapper ledgerMapper,
            GoldRegionMapper regionMapper) {
        return new GoldBeanApplicationService(
                new GoldBeanProperties(true, true, 60, 60, 20, 100, 99_800, 7, 50,
                        true, "normal_member_998", "gold_bean", 100, 10_000,
                        "机器人权益服务群", ""),
                accountMapper,
                mock(GoldBeanReferralMapper.class),
                ledgerMapper,
                mock(GoldBeanOrderMapper.class),
                regionMapper,
                mock(GoldBeanPlatformInviteService.class));
    }

    private GoldBeanAccountEntity account(String recipient) {
        GoldBeanAccountEntity account = new GoldBeanAccountEntity();
        account.setTenantId(2L);
        account.setUserId(3L);
        account.setMemberLevel("ordinary");
        account.setHistoricalLevel("ordinary");
        account.setDirectReferralCount(0);
        account.setRegistrationFeeStatus("PAID");
        account.setRegistrationFeeRecipient(recipient);
        account.setDigitalBankBalance(BigDecimal.ZERO);
        account.setTradingBalance(BigDecimal.ZERO);
        account.setDailyRewardStartAt(LocalDateTime.now());
        account.setDailyRewardLastAt(LocalDateTime.now());
        account.setDailyRewardDays(20);
        account.setTradeLimitPercent(100);
        account.setStatus("ACTIVE");
        account.setDeleted(0);
        account.setVersion(0);
        return account;
    }
}
