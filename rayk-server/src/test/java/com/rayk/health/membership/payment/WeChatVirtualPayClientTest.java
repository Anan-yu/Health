package com.rayk.health.membership.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.membership.config.MembershipProperties;
import com.rayk.health.security.wechat.WeChatSessionKeyStore;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class WeChatVirtualPayClientTest {
    @Test
    void usesTheExactWechatVirtualPaymentApiNameForPaySignature() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("rayk:wechat:session-key:app-id:1001")).thenReturn("session-key");

        MembershipProperties.WeChatVirtualPayProperties virtualPay =
                new MembershipProperties.WeChatVirtualPayProperties(
                        "app-id",
                        "merchant-id",
                        "offer-id",
                        "app-key",
                        "",
                        0,
                        "short_series_goods",
                        "vip_year_399",
                        "https://example.test/notify");
        MembershipProperties properties =
                new MembershipProperties(
                        true,
                        true,
                        3,
                        3,
                        1,
                        3,
                        3,
                        3,
                        false,
                        MembershipProperties.WeChatPayProperties.empty(),
                        virtualPay);
        WeChatVirtualPayClient client =
                new WeChatVirtualPayClient(
                        properties, new WeChatSessionKeyStore(redis), new ObjectMapper());

        var payment = client.createGoodsPayment("M202608201719abc123", 39900, 1001L);

        assertThat(payment.paySig())
                .isEqualTo(hmacSha256("app-key", "requestVirtualPayment&" + payment.signData()));
        assertThat(payment.signature()).isEqualTo(hmacSha256("session-key", payment.signData()));
        assertThat(payment.signData()).doesNotContain("/requestVirtualPayment");
    }

    private static String hmacSha256(String key, String message)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    }
}
