package com.rayk.health.membership.controller;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.goldbean.application.GoldBeanPaymentService;
import com.rayk.health.goldbean.application.GoldBeanTradeService;
import com.rayk.health.membership.application.MembershipApplicationService;
import com.rayk.health.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Receives the JSON goods-delivery push from WeChat Mini Program virtual payment. */
@RestController
@RequestMapping("/api/payments/wechat/virtual")
public class WechatVirtualPayNotificationController {
    private final MembershipApplicationService membershipService;
    private final GoldBeanPaymentService goldBeanPaymentService;
    private final GoldBeanTradeService goldBeanTradeService;

    /** Token used by the mini-program message-push URL verification handshake. */
    @Value("${WECHAT_VIRTUAL_PUSH_TOKEN:}")
    private String pushToken;

    public WechatVirtualPayNotificationController(
            MembershipApplicationService membershipService,
            GoldBeanPaymentService goldBeanPaymentService,
            GoldBeanTradeService goldBeanTradeService) {
        this.membershipService = membershipService;
        this.goldBeanPaymentService = goldBeanPaymentService;
        this.goldBeanTradeService = goldBeanTradeService;
    }

    @GetMapping("/notify")
    public ResponseEntity<String> verify(
            @RequestParam(required = false) String signature,
            @RequestParam(required = false) String timestamp,
            @RequestParam(required = false) String nonce,
            @RequestParam(required = false) String echostr) {
        if (!validPushSignature(signature, timestamp, nonce)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("forbidden");
        }
        return ResponseEntity.ok(echostr == null ? "" : echostr);
    }

    @PostMapping("/notify")
    public ResponseEntity<Map<String, Object>> notify(
            @RequestParam(required = false) String signature,
            @RequestParam(required = false) String timestamp,
            @RequestParam(required = false) String nonce,
            @RequestBody String body) {
        try {
            // The virtual-payment delivery callback is authenticated by the content-level
            // payEventSig. Some console versions POST directly to the configured callback URL
            // without the standard message-push query parameters used by the GET handshake.
            // Validate those parameters when present, but do not reject a valid delivery only
            // because that optional message-push envelope is absent.
            if (!validPostPushSignature(signature, timestamp, nonce)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(failure("消息推送验签失败"));
            }
            boolean messagePushAuthenticated = hasCompleteMessagePushParameters(signature, timestamp, nonce);
            TenantContext.executeReadWithoutTenant(() -> {
                if (goldBeanTradeService.isGoldBeanTradeNotification(body)) {
                    goldBeanTradeService.handleVirtualPaymentNotification(body, messagePushAuthenticated);
                } else if (goldBeanPaymentService.isGoldBeanNotification(body)) {
                    goldBeanPaymentService.handleVirtualPaymentNotification(body, messagePushAuthenticated);
                } else {
                    membershipService.handleVirtualPaymentNotification(body, messagePushAuthenticated);
                }
                return null;
            });
            return ResponseEntity.ok(messagePushAuthenticated ? standardMessagePushSuccess() : success());
        } catch (BusinessException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(failure("订单处理失败"));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failure("稍后重试"));
        }
    }

    private boolean validPushSignature(String signature, String timestamp, String nonce) {
        // Keep local installations without a configured push token backward compatible.
        // Production/remote environments that enable the real virtual-payment flow must set it.
        if (!StringUtils.hasText(pushToken)) {
            return true;
        }
        if (!StringUtils.hasText(signature)
                || !StringUtils.hasText(timestamp)
                || !StringUtils.hasText(nonce)) {
            return false;
        }
        try {
            String[] values = {pushToken.trim(), timestamp.trim(), nonce.trim()};
            Arrays.sort(values);
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            String expected = toHex(digest.digest(String.join("", values).getBytes(StandardCharsets.UTF_8)));
            byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
            byte[] actualBytes = signature.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expectedBytes, actualBytes);
        } catch (NoSuchAlgorithmException exception) {
            return false;
        }
    }

    private boolean validPostPushSignature(String signature, String timestamp, String nonce) {
        if (!hasMessagePushParameters(signature, timestamp, nonce)) {
            return true;
        }
        return validPushSignature(signature, timestamp, nonce);
    }

    private boolean hasMessagePushParameters(String signature, String timestamp, String nonce) {
        return StringUtils.hasText(signature)
                || StringUtils.hasText(timestamp)
                || StringUtils.hasText(nonce);
    }

    private boolean hasCompleteMessagePushParameters(String signature, String timestamp, String nonce) {
        return StringUtils.hasText(signature)
                && StringUtils.hasText(timestamp)
                && StringUtils.hasText(nonce);
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private Map<String, Object> success() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("returnCode", "0");
        response.put("returnMessage", "success");
        response.put("data", "ok");
        response.put("requestId", requestId());
        return response;
    }

    private Map<String, Object> standardMessagePushSuccess() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ErrCode", 0);
        response.put("ErrMsg", "success");
        return response;
    }

    private Map<String, Object> failure(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("returnCode", "1");
        response.put("returnMessage", message);
        response.put("data", "fail");
        response.put("requestId", requestId());
        response.put("ErrCode", 1);
        response.put("ErrMsg", message);
        return response;
    }

    private String requestId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
