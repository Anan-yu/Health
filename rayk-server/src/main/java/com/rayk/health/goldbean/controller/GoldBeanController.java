package com.rayk.health.goldbean.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.goldbean.application.GoldBeanApplicationService;
import com.rayk.health.goldbean.dto.OpenGoldRegionRequest;
import com.rayk.health.goldbean.dto.RegisterGoldBeanRequest;
import com.rayk.health.goldbean.vo.GoldBeanLedgerVo;
import com.rayk.health.goldbean.vo.GoldBeanSummaryVo;
import com.rayk.health.goldbean.vo.GoldRegionVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/gold-bean")
@PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
public class GoldBeanController {
    private final GoldBeanApplicationService service;

    public GoldBeanController(GoldBeanApplicationService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ApiResponse<GoldBeanSummaryVo> summary() {
        return ApiResponse.success(service.summary());
    }

    @PostMapping("/register")
    public ApiResponse<GoldBeanSummaryVo> register(
            @Valid @RequestBody(required = false) RegisterGoldBeanRequest request) {
        return ApiResponse.success(service.register(request));
    }

    @GetMapping("/ledger")
    public ApiResponse<List<GoldBeanLedgerVo>> ledger() {
        return ApiResponse.success(service.ledger());
    }

    @PostMapping("/regions")
    public ApiResponse<GoldRegionVo> openRegion(@Valid @RequestBody OpenGoldRegionRequest request) {
        return ApiResponse.success(service.openRegion(request));
    }

    @GetMapping("/regions/mine")
    public ApiResponse<GoldRegionVo> myRegion() {
        return ApiResponse.success(service.myRegion());
    }
}
