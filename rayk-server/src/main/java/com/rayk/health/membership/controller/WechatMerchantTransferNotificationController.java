package com.rayk.health.membership.controller;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.goldbean.application.GoldBeanReferralAuthorizationService;
import com.rayk.health.membership.payment.WeChatPayClient;
import com.rayk.health.membership.payment.WechatMerchantTransferAuthorizationNotification;
import com.wechat.pay.java.core.exception.MalformedMessageException;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.RequestParam;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public, signature-verified callback for one-time merchant-transfer receive authorization. */
@RestController
@RequestMapping("/api/payments/wechat/merchant-transfer")
public class WechatMerchantTransferNotificationController {
    private final WeChatPayClient weChatPayClient;
    private final GoldBeanReferralAuthorizationService authorizationService;

    public WechatMerchantTransferNotificationController(
            WeChatPayClient weChatPayClient,
            GoldBeanReferralAuthorizationService authorizationService) {
        this.weChatPayClient = weChatPayClient;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/authorization-notify")
    public ResponseEntity<Map<String, String>> authorizationNotify(
            @RequestHeader("Wechatpay-Serial") String serialNumber,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader(value = "Wechatpay-Signature-Type", required = false) String signType,
            @RequestBody String body) {
        try {
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(serialNumber)
                    .signature(signature)
                    .nonce(nonce)
                    .timestamp(timestamp)
                    .signType(signType)
                    .body(body)
                    .build();
            WechatMerchantTransferAuthorizationNotification result = weChatPayClient.parseNotification(
                    requestParam, WechatMerchantTransferAuthorizationNotification.class);
            authorizationService.handleAuthorizationNotification(result);
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (ValidationException | MalformedMessageException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", "FAIL", "message", "签名校验失败"));
        } catch (BusinessException | IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", "FAIL", "message", "授权通知处理失败"));
        } catch (RuntimeException exception) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("code", "FAIL", "message", "稍后重试"));
        }
    }
}
