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
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
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

    public MembershipPaymentVo createGoodsPayment(String orderNo, int amountCent, long userId) {
        MembershipProperties.WeChatVirtualPayProperties pay = properties.wechatVirtualPay();
        if (!configured()) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_NOT_CONFIGURED);
        }
        String sessionKey = sessionKeyStore.get(pay.appId(), userId);
        if (!StringUtils.hasText(sessionKey)) {
            throw new BusinessException(ErrorCode.WECHAT_PAYMENT_SESSION_EXPIRED);
        }

        Map<String, Object> signData = new LinkedHashMap<>();
        signData.put("offerId", pay.offerId());
        signData.put("buyQuantity", 1);
        signData.put("env", pay.env());
        signData.put("currencyType", "CNY");
        signData.put("productId", pay.productId());
        signData.put("goodsPrice", amountCent);
        signData.put("outTradeNo", orderNo);
        signData.put("attach", "membership:" + orderNo);

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
