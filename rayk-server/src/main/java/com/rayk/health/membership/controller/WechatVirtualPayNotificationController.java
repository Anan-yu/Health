package com.rayk.health.membership.controller;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.membership.application.MembershipApplicationService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Receives the JSON goods-delivery push from WeChat Mini Program virtual payment. */
@RestController
@RequestMapping("/api/payments/wechat/virtual")
public class WechatVirtualPayNotificationController {
    private final MembershipApplicationService membershipService;

    public WechatVirtualPayNotificationController(MembershipApplicationService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping("/notify")
    public ResponseEntity<Map<String, Object>> notify(@RequestBody String body) {
        try {
            membershipService.handleVirtualPaymentNotification(body);
            return ResponseEntity.ok(success());
        } catch (BusinessException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(failure("订单处理失败"));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failure("稍后重试"));
        }
    }

    private Map<String, Object> success() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ErrCode", 0);
        response.put("ErrMsg", "success");
        return response;
    }

    private Map<String, Object> failure(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ErrCode", 1);
        response.put("ErrMsg", message);
        return response;
    }
}
