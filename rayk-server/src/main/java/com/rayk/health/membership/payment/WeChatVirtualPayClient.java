package com.rayk.health.membership.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.membership.config.MembershipProperties;
import com.rayk.health.membership.vo.MembershipPaymentVo;
import com.rayk.health.security.wechat.WeChatSessionKeyStore;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Builds the server-side signatures required by wx.requestVirtualPayment. */
@Service
public class WeChatVirtualPayClient {
    // WeChat signs the API name without a leading slash:
    // HMAC-SHA256(appKey, "requestVirtualPayment&" + signData).
    private static final String PAYMENT_URI = "requestVirtualPayment";

    private final MembershipProperties properties;
    private final WeChatSessionKeyStore sessionKeyStore;
    private final ObjectMapper objectMapper;

    public WeChatVirtualPayClient(
            MembershipProperties properties,
            WeChatSessionKeyStore sessionKeyStore,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.sessionKeyStore = sessionKeyStore;
        this.objectMapper = objectMapper;
    }

    public boolean configured() {
        return properties.wechatVirtualPay().configured();
    }

    public MembershipProperties.WeChatVirtualPayProperties paymentProperties() {
        return properties.wechatVirtualPay();
    }

    public MembershipPaymentVo createGoodsPayment(String orderNo, int amountCent, long userId) {
        MembershipProperties.WeChatVirtualPayProperties pay = properties.wechatVirtualPay();
        return createGoodsPayment(orderNo, 1, pay.productId(), amountCent, userId, "membership:" + orderNo);
    }

    public boolean configuredFor(String productId) {
        MembershipProperties.WeChatVirtualPayProperties pay = properties.wechatVirtualPay();
        return StringUtils.hasText(productId)
                && StringUtils.hasText(pay.appId())
                && StringUtils.hasText(pay.merchantId())
                && StringUtils.hasText(pay.offerId())
                && StringUtils.hasText(pay.appKeyForCurrentEnv())
                && pay.env() >= 0
                && pay.env() <= 1
                && "short_series_goods".equals(pay.mode());
    }

    /** Verifies the HMAC that the virtual-payment platform puts on its delivery payload. */
    public boolean verifyPaymentEventSignature(String event, String payload, String payEventSig) {
        MembershipProperties.WeChatVirtualPayProperties pay = properties.wechatVirtualPay();
        if (!StringUtils.hasText(event)
                || !StringUtils.hasText(payload)
                || !StringUtils.hasText(payEventSig)
                || !StringUtils.hasText(pay.appKeyForCurrentEnv())) {
            return false;
        }
        try {
            String expected = hmacSha256(pay.appKeyForCurrentEnv(), event + "&" + payload);
            byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
            byte[] actualBytes = payEventSig.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expectedBytes, actualBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            return false;
        }
    }

    public MembershipPaymentVo createGoodsPayment(
            String orderNo,
            int buyQuantity,
            String productId,
            int goodsPriceCent,
            long userId,
            String attach) {
        MembershipProperties.WeChatVirtualPayProperties pay = properties.wechatVirtualPay();
        if (!configuredFor(productId) || buyQuantity <= 0 || goodsPriceCent <= 0) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_UNAVAILABLE);
        }
        String sessionKey = sessionKeyStore.get(pay.appId(), userId);
        if (!StringUtils.hasText(sessionKey)) {
            throw new BusinessException(ErrorCode.WECHAT_PAYMENT_SESSION_EXPIRED);
        }

        Map<String, Object> signData = new LinkedHashMap<>();
        signData.put("offerId", pay.offerId());
        signData.put("buyQuantity", buyQuantity);
        signData.put("env", pay.env());
        signData.put("currencyType", "CNY");
        signData.put("productId", productId);
        signData.put("goodsPrice", goodsPriceCent);
        signData.put("outTradeNo", orderNo);
        signData.put("attach", StringUtils.hasText(attach) ? attach : "gold-bean:" + orderNo);

        try {
            String signDataJson = objectMapper.writeValueAsString(signData);
            String paySig = hmacSha256(pay.appKeyForCurrentEnv(), PAYMENT_URI + "&" + signDataJson);
            String signature = hmacSha256(sessionKey, signDataJson);
            return new MembershipPaymentVo(pay.mode(), signDataJson, paySig, signature);
        } catch (JsonProcessingException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_UNAVAILABLE);
        }
    }

    private String hmacSha256(String key, String message)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }
}
