package com.rayk.health.membership.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.membership.application.MembershipApplicationService;
import com.rayk.health.membership.dto.CreateMembershipOrderRequest;
import com.rayk.health.membership.dto.PayMembershipOrderRequest;
import com.rayk.health.membership.dto.MembershipDevelopmentSwitchRequest;
import com.rayk.health.membership.vo.MembershipBenefitVo;
import com.rayk.health.membership.vo.MembershipOrderVo;
import com.rayk.health.membership.vo.MembershipPaymentVo;
import com.rayk.health.membership.vo.MembershipPlanVo;
import com.rayk.health.membership.vo.MembershipSummaryVo;
import com.rayk.health.membership.vo.MembershipUsageVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/membership")
@PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
public class MembershipController {
    private final MembershipApplicationService service;

    public MembershipController(MembershipApplicationService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ApiResponse<MembershipSummaryVo> summary() {
        return ApiResponse.success(service.summary());
    }

    @PostMapping("/dev/switch")
    public ApiResponse<MembershipSummaryVo> switchForDevelopment(
            @Valid @RequestBody MembershipDevelopmentSwitchRequest request) {
        return ApiResponse.success(service.switchForDevelopment(request.target()));
    }

    @GetMapping("/plans")
    public ApiResponse<List<MembershipPlanVo>> plans() {
        return ApiResponse.success(service.plans());
    }

    @GetMapping("/benefits")
    public ApiResponse<List<MembershipBenefitVo>> benefits() {
        return ApiResponse.success(service.benefits());
    }

    @GetMapping("/usage")
    public ApiResponse<List<MembershipUsageVo>> usage() {
        return ApiResponse.success(service.usage());
    }

    @PostMapping("/orders")
    public ApiResponse<MembershipOrderVo> createOrder(
            @Valid @RequestBody CreateMembershipOrderRequest request) {
        return ApiResponse.success(service.createOrder(request));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<MembershipOrderVo> order(@PathVariable String orderNo) {
        return ApiResponse.success(service.order(orderNo));
    }

    @PostMapping("/orders/{orderNo}/pay")
    public ApiResponse<MembershipOrderVo> pay(
            @PathVariable String orderNo,
            @RequestBody(required = false) PayMembershipOrderRequest request) {
        return ApiResponse.success(service.pay(orderNo, request == null ? new PayMembershipOrderRequest("SIMULATED") : request));
    }

    @PostMapping("/orders/{orderNo}/wechat-pay")
    public ApiResponse<MembershipPaymentVo> wechatPay(@PathVariable String orderNo) {
        return ApiResponse.success(service.createWechatPayment(orderNo));
    }
}
