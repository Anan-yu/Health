package com.rayk.health.mall.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.mall.application.MallApplicationService;
import com.rayk.health.mall.dto.MallAddressRequest;
import com.rayk.health.mall.dto.MallOrderRequest;
import com.rayk.health.mall.vo.MallAddressVo;
import com.rayk.health.mall.vo.MallOrderVo;
import com.rayk.health.mall.vo.MallPaymentVo;
import com.rayk.health.mall.vo.MallProductVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mall")
public class MallController {
    private final MallApplicationService service;

    public MallController(MallApplicationService service) {
        this.service = service;
    }

    @GetMapping("/products")
    @PreAuthorize("hasRole('PLATFORM_ADMIN') or (hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER')")
    public ApiResponse<List<MallProductVo>> products() {
        return ApiResponse.success(service.products(false));
    }

    @GetMapping("/products/{productId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN') or (hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER')")
    public ApiResponse<MallProductVo> product(@PathVariable String productId) {
        return ApiResponse.success(service.product(productId));
    }

    @GetMapping("/addresses")
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public ApiResponse<List<MallAddressVo>> addresses() {
        return ApiResponse.success(service.addresses());
    }

    @PostMapping("/addresses")
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public ApiResponse<MallAddressVo> createAddress(@Valid @RequestBody MallAddressRequest request) {
        return ApiResponse.success(service.saveAddress(null, request));
    }

    @PutMapping("/addresses/{addressId}")
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public ApiResponse<MallAddressVo> updateAddress(
            @PathVariable String addressId, @Valid @RequestBody MallAddressRequest request) {
        return ApiResponse.success(service.saveAddress(addressId, request));
    }

    @DeleteMapping("/addresses/{addressId}")
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public ApiResponse<Void> deleteAddress(@PathVariable String addressId) {
        service.deleteAddress(addressId);
        return ApiResponse.success(null);
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public ApiResponse<MallOrderVo> createOrder(@Valid @RequestBody MallOrderRequest request) {
        return ApiResponse.success(service.createOrder(request));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public ApiResponse<List<MallOrderVo>> orders() {
        return ApiResponse.success(service.orders());
    }

    @GetMapping("/orders/{orderNo}")
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public ApiResponse<MallOrderVo> order(@PathVariable String orderNo) {
        return ApiResponse.success(service.order(orderNo));
    }

    @PostMapping("/orders/{orderNo}/cancel")
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public ApiResponse<Void> cancel(@PathVariable String orderNo) {
        service.cancelOrder(orderNo);
        return ApiResponse.success(null);
    }

    @PostMapping("/orders/{orderNo}/wechat-pay")
    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    public ApiResponse<MallPaymentVo> wechatPay(@PathVariable String orderNo) {
        return ApiResponse.success(service.createWechatPayment(orderNo));
    }
}
