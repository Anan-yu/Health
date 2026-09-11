package com.rayk.health.goldbean.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanTradeEntity;
import com.rayk.health.goldbean.entity.GoldBeanTradeListingEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanTradeListingMapper;
import com.rayk.health.goldbean.mapper.GoldBeanTradeMapper;
import com.rayk.health.membership.config.MembershipProperties;
import com.rayk.health.membership.payment.WeChatMerchantTransferClient;
import com.rayk.health.membership.payment.WeChatPayClient;
import com.rayk.health.membership.payment.WeChatVirtualPayClient;
import com.rayk.health.security.service.CurrentPrincipal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class GoldBeanTradeServiceTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sellerCannotListMoreThanCurrentTradeLimit() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        GoldBeanAccountEntity account = account(20, 50);
        when(goldBeanService.available()).thenReturn(true);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(listingMapper.selectList(any())).thenReturn(List.of());
        when(tradeMapper.selectList(any())).thenReturn(List.of());
        authenticate(100L, 2L);

        GoldBeanTradeService service = service(goldBeanService, accountMapper, listingMapper, tradeMapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create(
                        new com.rayk.health.goldbean.dto.CreateGoldBeanTradeListingRequest(11)))
                .isInstanceOf(com.rayk.health.common.exception.BusinessException.class)
                .satisfies(error -> assertThat(((com.rayk.health.common.exception.BusinessException) error).getErrorCode())
                        .isEqualTo(com.rayk.health.common.exception.ErrorCode.GOLD_BEAN_TRADE_LIMIT_EXCEEDED));
    }

    @Test
    void legendarySellerCannotCreateListing() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanLegendaryService legendaryService = mock(GoldBeanLegendaryService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        when(goldBeanService.available()).thenReturn(true);
        when(legendaryService.isEligible(100L)).thenReturn(true);
        authenticate(100L, 2L);

        GoldBeanTradeService service = service(
                goldBeanService, legendaryService, accountMapper, listingMapper, tradeMapper);

        assertThatThrownBy(() -> service.create(
                        new com.rayk.health.goldbean.dto.CreateGoldBeanTradeListingRequest(1)))
                .isInstanceOf(com.rayk.health.common.exception.BusinessException.class)
                .satisfies(error -> assertThat(((com.rayk.health.common.exception.BusinessException) error).getErrorCode())
                        .isEqualTo(com.rayk.health.common.exception.ErrorCode.GOLD_BEAN_TRADE_LEGENDARY_SELL_FORBIDDEN));
    }

    @Test
    void createsFixedPriceListingWithoutTouchingWalletBalance() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        GoldBeanAccountEntity account = account(20, 100);
        when(goldBeanService.available()).thenReturn(true);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(listingMapper.selectList(any())).thenReturn(List.of());
        when(tradeMapper.selectList(any())).thenReturn(List.of());
        authenticate(100L, 2L);

        GoldBeanTradeService service = service(goldBeanService, accountMapper, listingMapper, tradeMapper);
        var listing = service.create(new com.rayk.health.goldbean.dto.CreateGoldBeanTradeListingRequest(10));

        assertThat(listing.unitPriceCent()).isEqualTo(100);
        assertThat(listing.remainingQuantity()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(account.getTradingBalance()).isEqualByComparingTo(BigDecimal.valueOf(20));
        verify(listingMapper).insert(any(GoldBeanTradeListingEntity.class));
    }

    @Test
    void canListDigitalBankBeansAndKeepsTheBucketOnTheListing() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        GoldBeanAccountEntity account = account(20, 100);
        account.setDigitalBankBalance(BigDecimal.valueOf(30));
        when(goldBeanService.available()).thenReturn(true);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(listingMapper.selectList(any())).thenReturn(List.of());
        when(tradeMapper.selectList(any())).thenReturn(List.of());
        authenticate(100L, 2L);

        GoldBeanTradeService service = service(goldBeanService, accountMapper, listingMapper, tradeMapper);
        var listing = service.create(new com.rayk.health.goldbean.dto.CreateGoldBeanTradeListingRequest(
                BigDecimal.valueOf(10), "DIGITAL_BANK"));

        assertThat(listing.bucket()).isEqualTo("DIGITAL_BANK");
        assertThat(listing.remainingQuantity()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(account.getDigitalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(30));
        verify(listingMapper).insert(any(GoldBeanTradeListingEntity.class));
    }

    @Test
    void sellerCannotListMoreThanTradingBalanceWhenLimitIsFull() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        GoldBeanAccountEntity account = account(20, 100);
        when(goldBeanService.available()).thenReturn(true);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(listingMapper.selectList(any())).thenReturn(List.of());
        when(tradeMapper.selectList(any())).thenReturn(List.of());
        authenticate(100L, 2L);

        GoldBeanTradeService service = service(goldBeanService, accountMapper, listingMapper, tradeMapper);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.create(
                        new com.rayk.health.goldbean.dto.CreateGoldBeanTradeListingRequest(21)))
                .isInstanceOf(com.rayk.health.common.exception.BusinessException.class)
                .satisfies(error -> assertThat(((com.rayk.health.common.exception.BusinessException) error).getErrorCode())
                        .isEqualTo(com.rayk.health.common.exception.ErrorCode.GOLD_BEAN_TRADE_LIMIT_EXCEEDED));
    }

    @Test
    void demotedMemberCanKeepTradingDigitalBankBeansBetweenMembers() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        GoldBeanAccountEntity account = account(0, 50);
        account.setMemberLevel("SILVER");
        account.setDigitalBankBalance(BigDecimal.valueOf(20));
        when(goldBeanService.available()).thenReturn(true);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(listingMapper.selectList(any())).thenReturn(List.of());
        when(tradeMapper.selectList(any())).thenReturn(List.of());
        authenticate(100L, 2L);

        GoldBeanTradeService service = service(goldBeanService, accountMapper, listingMapper, tradeMapper);
        var listing = service.create(new com.rayk.health.goldbean.dto.CreateGoldBeanTradeListingRequest(
                BigDecimal.valueOf(10), "DIGITAL_BANK"));

        assertThat(listing.bucket()).isEqualTo("DIGITAL_BANK");
        assertThat(listing.remainingQuantity()).isEqualByComparingTo(BigDecimal.valueOf(10));
        verify(listingMapper).insert(any(GoldBeanTradeListingEntity.class));
    }

    @Test
    void legendaryBuyerCannotReserveTradingBeanListing() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanLegendaryService legendaryService = mock(GoldBeanLegendaryService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        GoldBeanAccountEntity account = account(100, 100);
        GoldBeanTradeListingEntity listing = listing(200L, "TRADING", 10L);
        when(goldBeanService.available()).thenReturn(true);
        when(legendaryService.isEligible(100L)).thenReturn(true);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(listingMapper.selectOne(any())).thenReturn(listing);
        when(tradeMapper.selectList(any())).thenReturn(List.of());
        authenticate(100L, 2L);

        GoldBeanTradeService service = service(
                goldBeanService, legendaryService, accountMapper, listingMapper, tradeMapper);

        assertThatThrownBy(() -> service.buy("10", new com.rayk.health.goldbean.dto.BuyGoldBeanTradeRequest(1)))
                .isInstanceOf(com.rayk.health.common.exception.BusinessException.class)
                .satisfies(error -> assertThat(((com.rayk.health.common.exception.BusinessException) error).getErrorCode())
                        .isEqualTo(com.rayk.health.common.exception.ErrorCode.GOLD_BEAN_TRADE_LEGENDARY_DIGITAL_ONLY));
    }

    @Test
    void ordinaryBuyerCannotReserveListingFromAnotherRegion() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanLegendaryService legendaryService = mock(GoldBeanLegendaryService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        GoldBeanAccountEntity buyer = account(100, 100);
        buyer.setUserId(100L);
        buyer.setCity("河南省 / 驻马店市");
        GoldBeanAccountEntity seller = account(100, 100);
        seller.setUserId(200L);
        seller.setCity("河南省 / 郑州市");
        GoldBeanTradeListingEntity listing = listing(200L, "TRADING", 10L);
        listing.setRegionCity(seller.getCity());
        when(goldBeanService.available()).thenReturn(true);
        when(legendaryService.isEligible(100L)).thenReturn(false);
        when(accountMapper.selectOne(any())).thenReturn(buyer, seller);
        when(listingMapper.selectOne(any())).thenReturn(listing);
        authenticate(100L, 2L);

        GoldBeanTradeService service = service(
                goldBeanService, legendaryService, accountMapper, listingMapper, tradeMapper);

        assertThatThrownBy(() -> service.buy("10", new com.rayk.health.goldbean.dto.BuyGoldBeanTradeRequest(1)))
                .isInstanceOf(com.rayk.health.common.exception.BusinessException.class)
                .satisfies(error -> assertThat(((com.rayk.health.common.exception.BusinessException) error).getErrorCode())
                        .isEqualTo(com.rayk.health.common.exception.ErrorCode.GOLD_BEAN_TRADE_REGION_MISMATCH));
    }

    @Test
    void ordinaryBuyerCanReserveListingFromTheSameRegion() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanLegendaryService legendaryService = mock(GoldBeanLegendaryService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        GoldBeanAccountEntity buyer = account(100, 100);
        buyer.setUserId(100L);
        buyer.setCity("河南省 / 驻马店市");
        GoldBeanAccountEntity seller = account(100, 100);
        seller.setUserId(200L);
        seller.setCity("河南省 / 驻马店市");
        GoldBeanTradeListingEntity listing = listing(200L, "TRADING", 10L);
        listing.setRegionCity(seller.getCity());
        when(goldBeanService.available()).thenReturn(true);
        when(legendaryService.isEligible(100L)).thenReturn(false);
        when(accountMapper.selectOne(any())).thenReturn(buyer, seller);
        when(listingMapper.selectOne(any())).thenReturn(listing);
        when(tradeMapper.selectList(any())).thenReturn(List.of());
        authenticate(100L, 2L);

        GoldBeanTradeService service = service(
                goldBeanService, legendaryService, accountMapper, listingMapper, tradeMapper);

        service.buy("10", new com.rayk.health.goldbean.dto.BuyGoldBeanTradeRequest(1));

        var captured = org.mockito.ArgumentCaptor.forClass(GoldBeanTradeEntity.class);
        verify(tradeMapper).insert(captured.capture());
        assertThat(captured.getValue().getUnitPriceCent()).isEqualTo(100L);
        assertThat(captured.getValue().getPaymentUnitPriceCent()).isEqualTo(112L);
        assertThat(captured.getValue().getTotalAmount()).isEqualTo(100L);
        assertThat(captured.getValue().getPaymentAmount()).isEqualTo(112L);
    }

    @Test
    void ordinaryBuyerCannotReserveDigitalBankListing() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanLegendaryService legendaryService = mock(GoldBeanLegendaryService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        GoldBeanAccountEntity account = account(100, 100);
        GoldBeanTradeListingEntity listing = listing(200L, "DIGITAL_BANK", 10L);
        when(goldBeanService.available()).thenReturn(true);
        when(legendaryService.isEligible(100L)).thenReturn(false);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(listingMapper.selectOne(any())).thenReturn(listing);
        when(tradeMapper.selectList(any())).thenReturn(List.of());
        authenticate(100L, 2L);

        GoldBeanTradeService service = service(
                goldBeanService, legendaryService, accountMapper, listingMapper, tradeMapper);

        assertThatThrownBy(() -> service.buy("10", new com.rayk.health.goldbean.dto.BuyGoldBeanTradeRequest(1)))
                .isInstanceOf(com.rayk.health.common.exception.BusinessException.class)
                .satisfies(error -> assertThat(((com.rayk.health.common.exception.BusinessException) error).getErrorCode())
                        .isEqualTo(com.rayk.health.common.exception.ErrorCode.GOLD_BEAN_TRADE_DIGITAL_BANK_LEGENDARY_ONLY));
    }

    @Test
    void legendaryBuyerReservesDigitalBankListingWithDigitalBankCreditMode() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanLegendaryService legendaryService = mock(GoldBeanLegendaryService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        GoldBeanAccountEntity account = account(100, 100);
        account.setDigitalBankBalance(BigDecimal.valueOf(100));
        GoldBeanTradeListingEntity listing = listing(200L, "DIGITAL_BANK", 10L);
        when(goldBeanService.available()).thenReturn(true);
        when(legendaryService.isEligible(100L)).thenReturn(true);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(listingMapper.selectOne(any())).thenReturn(listing);
        when(tradeMapper.selectList(any())).thenReturn(List.of());
        authenticate(100L, 2L);

        GoldBeanTradeService service = service(
                goldBeanService, legendaryService, accountMapper, listingMapper, tradeMapper);
        service.buy("10", new com.rayk.health.goldbean.dto.BuyGoldBeanTradeRequest(1));

        var captured = org.mockito.ArgumentCaptor.forClass(GoldBeanTradeEntity.class);
        verify(tradeMapper).insert(captured.capture());
        assertThat(captured.getValue().getBuyerCreditMode()).isEqualTo("DIGITAL_BANK");
    }

    @Test
    void ordinaryBuyerCompletionUsesBothLedgers() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        GoldBeanAccountEntity buyer = account(100, 0);
        GoldBeanAccountEntity seller = account(200, 100);
        seller.setUserId(200L);
        when(accountMapper.selectOne(any())).thenReturn(buyer, seller);

        GoldBeanTradeService service = service(
                goldBeanService, mock(GoldBeanLegendaryService.class), accountMapper, listingMapper, tradeMapper);
        GoldBeanTradeEntity trade = trade("TRADING", "SPLIT", 11L);
        service.completeTrade(trade);

        verify(goldBeanService).creditForTradeSplit(
                eq(buyer), eq(BigDecimal.valueOf(11)), eq("TRADE_SETTLEMENT_BUY"), eq(200L),
                eq("GBT-1:BUYER:SPLIT"), eq("已完成微信付款并获得金豆（一半进入可交易金豆、一半进入数字银行）"));
    }

    @Test
    void legendaryBuyerCompletionUsesDigitalBankOnly() {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanTradeListingMapper listingMapper = mock(GoldBeanTradeListingMapper.class);
        GoldBeanTradeMapper tradeMapper = mock(GoldBeanTradeMapper.class);
        GoldBeanAccountEntity buyer = account(100, 0);
        GoldBeanAccountEntity seller = account(200, 100);
        seller.setUserId(200L);
        when(accountMapper.selectOne(any())).thenReturn(buyer, seller);

        GoldBeanTradeService service = service(
                goldBeanService, mock(GoldBeanLegendaryService.class), accountMapper, listingMapper, tradeMapper);
        GoldBeanTradeEntity trade = trade("DIGITAL_BANK", "DIGITAL_BANK", 11L);
        service.completeTrade(trade);

        verify(goldBeanService).creditForTradeDigitalBank(
                eq(buyer), eq(BigDecimal.valueOf(11)), eq("TRADE_SETTLEMENT_BUY"), eq(200L),
                eq("GBT-1:BUYER:DIGITAL_BANK"), eq("已完成微信付款并获得数字银行金豆"));
    }

    private GoldBeanTradeService service(
            GoldBeanApplicationService goldBeanService,
            GoldBeanAccountMapper accountMapper,
            GoldBeanTradeListingMapper listingMapper,
            GoldBeanTradeMapper tradeMapper) {
        return service(goldBeanService, mock(GoldBeanLegendaryService.class), accountMapper, listingMapper, tradeMapper);
    }

    private GoldBeanTradeService service(
            GoldBeanApplicationService goldBeanService,
            GoldBeanLegendaryService legendaryService,
            GoldBeanAccountMapper accountMapper,
            GoldBeanTradeListingMapper listingMapper,
            GoldBeanTradeMapper tradeMapper) {
        MembershipProperties properties = new MembershipProperties(
                true, true, 3, 3, 1, 3, 3, 3, true,
                MembershipProperties.WeChatPayProperties.empty(),
                MembershipProperties.WeChatVirtualPayProperties.empty());
        GoldBeanProperties goldBeanProperties = new GoldBeanProperties(
                true, true, 60, 60, 20, 100, 99800, 7, 50, true,
                "", "", "gold_bean", 100, 10000, "机器人权益服务群", "");
        return new GoldBeanTradeService(goldBeanService, legendaryService, accountMapper, listingMapper, tradeMapper,
                mock(com.rayk.health.security.wechat.mapper.WeChatUserBindingMapper.class),
                new WeChatPayClient(properties), mock(WeChatMerchantTransferClient.class),
                mock(GoldBeanReferralAuthorizationService.class), properties, goldBeanProperties,
                mock(WeChatVirtualPayClient.class), new ObjectMapper(), false, "", "",
                "劳务报酬", "金豆交易服务", "金豆交易结算", 30);
    }

    private GoldBeanAccountEntity account(long tradingBalance, int limit) {
        GoldBeanAccountEntity account = new GoldBeanAccountEntity();
        account.setTenantId(2L);
        account.setUserId(100L);
        account.setRegistrationFeeStatus("PAID");
        account.setStatus("ACTIVE");
        account.setTradingBalance(BigDecimal.valueOf(tradingBalance));
        account.setTradeLimitPercent(limit);
        account.setDigitalBankBalance(BigDecimal.ZERO);
        account.setCity("河南省 / 驻马店市");
        account.setDailyRewardStartAt(LocalDateTime.now());
        return account;
    }

    private GoldBeanTradeListingEntity listing(long sellerUserId, String bucket, long quantity) {
        GoldBeanTradeListingEntity listing = new GoldBeanTradeListingEntity();
        listing.setId(10L);
        listing.setTenantId(2L);
        listing.setSellerUserId(sellerUserId);
        listing.setBucket(bucket);
        listing.setRegionCity("河南省 / 驻马店市");
        listing.setQuantity(BigDecimal.valueOf(quantity));
        listing.setRemainingQuantity(BigDecimal.valueOf(quantity));
        listing.setStatus("OPEN");
        listing.setDeleted(0);
        return listing;
    }

    private GoldBeanTradeEntity trade(String bucket, String buyerCreditMode, long quantity) {
        GoldBeanTradeEntity trade = new GoldBeanTradeEntity();
        trade.setId(1L);
        trade.setTenantId(2L);
        trade.setTradeNo("GBT-1");
        trade.setSellerUserId(200L);
        trade.setBuyerUserId(100L);
        trade.setBucket(bucket);
        trade.setBuyerCreditMode(buyerCreditMode);
        trade.setQuantity(BigDecimal.valueOf(quantity));
        trade.setStatus("TRANSFER_PENDING");
        return trade;
    }

    private void authenticate(long userId, long tenantId) {
        CurrentPrincipal principal = new CurrentPrincipal("jti", "customer", userId, tenantId,
                List.of("CUSTOMER"), List.of("self:health-record"), "CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
