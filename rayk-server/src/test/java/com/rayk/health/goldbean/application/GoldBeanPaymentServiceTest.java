package com.rayk.health.goldbean.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.dto.CreateGoldBeanPurchaseOrderRequest;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanOrderEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanOrderMapper;
import com.rayk.health.membership.config.MembershipProperties;
import com.rayk.health.membership.payment.WeChatMerchantTransferClient;
import com.rayk.health.membership.payment.WeChatPayClient;
import com.rayk.health.membership.payment.WeChatVirtualPayClient;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.wechat.entity.WeChatUserBindingEntity;
import com.rayk.health.security.wechat.mapper.WeChatUserBindingMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.security.core.context.SecurityContextHolder;

class GoldBeanPaymentServiceTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ORDER_NO = "GBP-LEGENDARY-1";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsVirtualPaymentWithoutTransactionIdBeforeCredit() {
        Fixture fixture = fixture();

        assertThatThrownBy(() -> fixture.service.handleVirtualPaymentNotification(
                        deliveryBody(null), true))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE));

        verify(fixture.goldBeanService, never()).completeLegendaryBankPurchase(
                anyLong(), anyLong(), any(BigDecimal.class), any());
        verify(fixture.orderMapper, never()).updateById(fixture.order);
    }

    @Test
    void rechecksLegendaryEligibilityImmediatelyBeforeCredit() {
        Fixture fixture = fixture();

        fixture.service.handleVirtualPaymentNotification(deliveryBody("wx-transaction-1"), true);

        InOrder order = inOrder(fixture.legendaryService, fixture.goldBeanService);
        order.verify(fixture.legendaryService).requireEligible(3L);
        order.verify(fixture.goldBeanService).completeLegendaryBankPurchase(
                2L, 3L, BigDecimal.valueOf(3), ORDER_NO);
        verify(fixture.orderMapper).updateById(fixture.order);
    }

    @Test
    void createsPlatformPurchaseOrderForEligiblePlatformAgent() {
        Fixture fixture = fixture();
        GoldBeanAccountEntity account = new GoldBeanAccountEntity();
        account.setRegistrationFeeStatus("PAID");
        account.setRegistrationFeeRecipient("PLATFORM");
        when(fixture.goldBeanService.ensureAccount(any())).thenReturn(account);
        authenticateCustomer();

        var result = fixture.service.createPurchaseOrder(new CreateGoldBeanPurchaseOrderRequest(3L));

        assertThat(result.orderType()).isEqualTo("GOLD_BEAN_PURCHASE");
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.amountCent()).isEqualTo(300);
        assertThat(result.paymentAmountCent()).isEqualTo(336);
        assertThat(result.goldBeanQuantity()).isEqualByComparingTo(BigDecimal.valueOf(3));
        verify(fixture.goldBeanService).settleForPurchase(account, 3L);
        verify(fixture.goldBeanService).requirePlatformPurchaseEligible(account);
        verify(fixture.orderMapper).insert(any(GoldBeanOrderEntity.class));
    }

    @Test
    void refusesNewPlatformPurchaseForLegendaryUser() {
        Fixture fixture = fixture();
        when(fixture.legendaryService.isEligible(3L)).thenReturn(true);
        authenticateCustomer();

        assertThatThrownBy(() -> fixture.service.createPurchaseOrder(
                        new CreateGoldBeanPurchaseOrderRequest(1L)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.GOLD_BEAN_PLATFORM_PURCHASE_DISABLED));

        verify(fixture.goldBeanService, never()).ensureAccount(any());
        verify(fixture.orderMapper, never()).insert(any(GoldBeanOrderEntity.class));
    }

    private static Fixture fixture() {
        GoldBeanOrderMapper orderMapper = mock(GoldBeanOrderMapper.class);
        GoldBeanOrderEntity order = new GoldBeanOrderEntity();
        order.setOrderNo(ORDER_NO);
        order.setOrderType("LEGENDARY_BANK_PURCHASE");
        order.setStatus("PENDING");
        order.setDeleted(0);
        order.setTenantId(2L);
        order.setCustomerId(3L);
        order.setProductId("gold_bean");
        order.setGoldBeanQuantity(BigDecimal.valueOf(3));
        order.setAmountCent(300);
        order.setPaymentAmountCent(336);
        when(orderMapper.selectOne(any())).thenReturn(order).thenReturn(null);

        WeChatUserBindingEntity binding = new WeChatUserBindingEntity();
        binding.setTenantId(2L);
        binding.setUserId(3L);
        binding.setAppId("wx-test-app");
        binding.setOpenid("openid-3");
        binding.setStatus("ACTIVE");
        binding.setDeleted(0);
        WeChatUserBindingMapper bindingMapper = mock(WeChatUserBindingMapper.class);
        when(bindingMapper.selectOne(any())).thenReturn(binding);

        MembershipProperties.WeChatVirtualPayProperties virtualProperties =
                new MembershipProperties.WeChatVirtualPayProperties(
                        "wx-test-app", "merchant-1", "offer-1", "app-key", "sandbox-key", 0,
                        "short_series_goods", "gold_bean", "https://example.test/notify");
        WeChatVirtualPayClient virtualPayClient = mock(WeChatVirtualPayClient.class);
        when(virtualPayClient.paymentProperties()).thenReturn(virtualProperties);
        when(virtualPayClient.configuredFor("gold_bean")).thenReturn(true);

        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        when(goldBeanService.available()).thenReturn(true);
        GoldBeanLegendaryService legendaryService = mock(GoldBeanLegendaryService.class);
        GoldBeanPaymentService service = new GoldBeanPaymentService(
                new GoldBeanProperties(
                        true, true, 60, 60, 20, 100, 99_800, 7, 50, true,
                        "normal_member_998", "test_member", "gold_bean", 100, 10_000,
                        "机器人权益服务群", ""),
                goldBeanService,
                mock(GoldBeanAccountMapper.class),
                orderMapper,
                bindingMapper,
                mock(WeChatPayClient.class),
                mock(WeChatMerchantTransferClient.class),
                mock(GoldBeanReferralAuthorizationService.class),
                new MembershipProperties(
                        true, true, 0, 0, 0, 0, 0, 0, true,
                        MembershipProperties.WeChatPayProperties.empty(), virtualProperties),
                virtualPayClient,
                OBJECT_MAPPER,
                mock(GoldBeanPlatformInviteService.class),
                legendaryService,
                mock(PlatformTransactionManager.class),
                false,
                "1005",
                "",
                "劳务报酬",
                "推荐注册服务",
                "推荐注册服务报酬",
                30);
        return new Fixture(service, goldBeanService, legendaryService, orderMapper, order);
    }

    private static String deliveryBody(String transactionId) {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("Event", "xpay_goods_deliver_notify");
        root.put("EventType", "TRANSACTION.SUCCESS");
        root.put("OutTradeNo", ORDER_NO);
        root.put("OpenId", "openid-3");
        root.put("Env", 0);
        ObjectNode goods = root.putObject("GoodsInfo");
        goods.put("ProductId", "gold_bean");
        goods.put("Quantity", 3);
        goods.put("ActualPrice", 112);
        if (transactionId != null) {
            root.putObject("WeChatPayInfo").put("TransactionId", transactionId);
        }
        return root.toString();
    }

    private static void authenticateCustomer() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 3L, 2L,
                        List.of("CUSTOMER"), List.of("self:health-record"), "CUSTOMER"), null));
    }

    private record Fixture(
            GoldBeanPaymentService service,
            GoldBeanApplicationService goldBeanService,
            GoldBeanLegendaryService legendaryService,
            GoldBeanOrderMapper orderMapper,
            GoldBeanOrderEntity order) {}
}
