package com.rayk.health.membership.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rayk.health.goldbean.application.GoldBeanPaymentService;
import com.rayk.health.goldbean.application.GoldBeanTradeService;
import com.rayk.health.membership.application.MembershipApplicationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

class WechatVirtualPayNotificationControllerTest {

    @Test
    void verifiesWechatMessagePushUrlWithSha1Signature() throws Exception {
        WechatVirtualPayNotificationController controller = controllerWithToken("push-token-2026");
        String timestamp = "1720000000";
        String nonce = "nonce-2026";
        String echostr = "wechat-verify";

        ResponseEntity<String> response = controller.verify(
                sha1("push-token-2026", timestamp, nonce), timestamp, nonce, echostr);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(echostr, response.getBody());
    }

    @Test
    void rejectsWechatMessagePushUrlWithWrongSignature() throws Exception {
        WechatVirtualPayNotificationController controller = controllerWithToken("push-token-2026");

        ResponseEntity<String> response = controller.verify(
                "0000000000000000000000000000000000000000",
                "1720000000",
                "nonce-2026",
                "wechat-verify");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void acceptsVirtualPaymentPostWithoutMessagePushQueryParameters() {
        MembershipApplicationService membership = mock(MembershipApplicationService.class);
        GoldBeanPaymentService goldBean = mock(GoldBeanPaymentService.class);
        GoldBeanTradeService trade = mock(GoldBeanTradeService.class);
        when(goldBean.isGoldBeanNotification("{}")).thenReturn(true);
        WechatVirtualPayNotificationController controller = new WechatVirtualPayNotificationController(
                membership, goldBean, trade);
        ReflectionTestUtils.setField(controller, "pushToken", "push-token-2026");

        ResponseEntity<Map<String, Object>> response = controller.notify(null, null, null, "{}");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void routesStandardJsonGoodsDeliveryAsAuthenticatedMessagePush() throws Exception {
        MembershipApplicationService membership = mock(MembershipApplicationService.class);
        GoldBeanPaymentService goldBean = mock(GoldBeanPaymentService.class);
        GoldBeanTradeService trade = mock(GoldBeanTradeService.class);
        String body = "{\"Event\":\"xpay_goods_deliver_notify\","
                + "\"OpenId\":\"openid\",\"OutTradeNo\":\"GBRPROBE123\","
                + "\"Env\":0,\"GoodsInfo\":{\"ProductId\":\"test_member\","
                + "\"Quantity\":1,\"ActualPrice\":100}}";
        when(goldBean.isGoldBeanNotification(body)).thenReturn(true);
        WechatVirtualPayNotificationController controller = new WechatVirtualPayNotificationController(
                membership, goldBean, trade);
        ReflectionTestUtils.setField(controller, "pushToken", "push-token-2026");
        String timestamp = "1720000000";
        String nonce = "nonce-2026";

        ResponseEntity<Map<String, Object>> response = controller.notify(
                sha1("push-token-2026", timestamp, nonce), timestamp, nonce, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(goldBean).handleVirtualPaymentNotification(body, true);
    }

    @Test
    void routesTradeGoodsDeliveryToTradeService() throws Exception {
        MembershipApplicationService membership = mock(MembershipApplicationService.class);
        GoldBeanPaymentService goldBean = mock(GoldBeanPaymentService.class);
        GoldBeanTradeService trade = mock(GoldBeanTradeService.class);
        String body = "{\"Event\":\"xpay_goods_deliver_notify\","
                + "\"OpenId\":\"openid\",\"OutTradeNo\":\"GBTPROBE123\","
                + "\"Env\":0,\"GoodsInfo\":{\"ProductId\":\"gold_bean\","
                + "\"Quantity\":1,\"ActualPrice\":100}}";
        when(trade.isGoldBeanTradeNotification(body)).thenReturn(true);
        WechatVirtualPayNotificationController controller = new WechatVirtualPayNotificationController(
                membership, goldBean, trade);
        ReflectionTestUtils.setField(controller, "pushToken", "push-token-2026");
        String timestamp = "1720000000";
        String nonce = "nonce-2026";

        ResponseEntity<Map<String, Object>> response = controller.notify(
                sha1("push-token-2026", timestamp, nonce), timestamp, nonce, body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(trade).handleVirtualPaymentNotification(body, true);
    }

    private WechatVirtualPayNotificationController controllerWithToken(String token) {
        WechatVirtualPayNotificationController controller = new WechatVirtualPayNotificationController(
                mock(MembershipApplicationService.class),
                mock(GoldBeanPaymentService.class),
                mock(GoldBeanTradeService.class));
        ReflectionTestUtils.setField(controller, "pushToken", token);
        return controller;
    }

    private String sha1(String... values) throws Exception {
        String[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        byte[] digest = MessageDigest.getInstance("SHA-1")
                .digest(String.join("", sorted).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            hex.append(String.format("%02x", value & 0xff));
        }
        return hex.toString();
    }
}
