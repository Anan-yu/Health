package com.rayk.health.membership.controller;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.membership.application.MembershipApplicationService;
import com.rayk.health.membership.payment.WeChatPayClient;
import com.rayk.health.mall.application.MallApplicationService;
import com.rayk.health.goldbean.application.GoldBeanPaymentService;
import com.rayk.health.goldbean.application.GoldBeanTradeService;
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

/** Public, signature-verified WeChat Pay callback endpoint. */
@RestController
@RequestMapping("/api/payments/wechat")
public class WechatPayNotificationController {
    private final WeChatPayClient weChatPayClient;
    private final MembershipApplicationService membershipService;
    private final MallApplicationService mallService;
    private final GoldBeanPaymentService goldBeanPaymentService;
    private final GoldBeanTradeService goldBeanTradeService;

    public WechatPayNotificationController(
            WeChatPayClient weChatPayClient,
            MembershipApplicationService membershipService,
            MallApplicationService mallService,
            GoldBeanPaymentService goldBeanPaymentService,
            GoldBeanTradeService goldBeanTradeService) {
        this.weChatPayClient = weChatPayClient;
        this.membershipService = membershipService;
        this.mallService = mallService;
        this.goldBeanPaymentService = goldBeanPaymentService;
        this.goldBeanTradeService = goldBeanTradeService;
    }

    @PostMapping("/notify")
    public ResponseEntity<Map<String, String>> notify(
            @RequestHeader("Wechatpay-Serial") String serialNumber,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader(value = "Wechatpay-Signature-Type", required = false) String signType,
            @RequestBody String body) {
        try {
            RequestParam requestParam =
                    new RequestParam.Builder()
                            .serialNumber(serialNumber)
                            .signature(signature)
                            .nonce(nonce)
                            .timestamp(timestamp)
                            .signType(signType)
                            .body(body)
                            .build();
            var transaction = weChatPayClient.parseNotification(requestParam);
            if (transaction != null
                    && transaction.getOutTradeNo() != null
                    && transaction.getOutTradeNo().startsWith("GBR")) {
                goldBeanPaymentService.handleWechatPayment(transaction);
            } else if (transaction != null
                    && transaction.getOutTradeNo() != null
                    && transaction.getOutTradeNo().startsWith("GBT")) {
                goldBeanTradeService.handleWechatPayment(transaction);
            } else if (transaction != null
                    && transaction.getOutTradeNo() != null
                    && transaction.getOutTradeNo().startsWith("G")) {
                mallService.handleWechatPayment(transaction);
            } else {
                membershipService.handleWechatPayment(transaction);
            }
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (ValidationException | MalformedMessageException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", "FAIL", "message", "签名校验失败"));
        } catch (BusinessException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("code", "FAIL", "message", "订单处理失败"));
        } catch (RuntimeException exception) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("code", "FAIL", "message", "稍后重试"));
        }
    }
}
