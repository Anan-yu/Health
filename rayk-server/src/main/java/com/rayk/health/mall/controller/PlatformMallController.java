package com.rayk.health.mall.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.mall.application.MallApplicationService;
import com.rayk.health.mall.dto.MallProductRequest;
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
@RequestMapping("/api/v1/platform/mall/products")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class PlatformMallController {
    private final MallApplicationService service;

    public PlatformMallController(MallApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<MallProductVo>> products() {
        return ApiResponse.success(service.products(true));
    }

    @PostMapping
    public ApiResponse<MallProductVo> create(@Valid @RequestBody MallProductRequest request) {
        return ApiResponse.success(service.saveProduct(null, request));
    }

    @PutMapping("/{productId}")
    public ApiResponse<MallProductVo> update(
            @PathVariable String productId, @Valid @RequestBody MallProductRequest request) {
        return ApiResponse.success(service.saveProduct(parseId(productId), request));
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> disable(@PathVariable String productId) {
        service.disableProduct(productId);
        return ApiResponse.success(null);
    }

    private Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new com.rayk.health.common.exception.BusinessException(
                    com.rayk.health.common.exception.ErrorCode.MALL_PRODUCT_NOT_FOUND);
        }
    }
}
