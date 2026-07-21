package com.rayk.health.indicator.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.indicator.application.IndicatorDictService;
import com.rayk.health.indicator.dto.AddAliasRequest;
import com.rayk.health.indicator.dto.CreateIndicatorRequest;
import com.rayk.health.indicator.dto.UpdateIndicatorRequest;
import com.rayk.health.indicator.vo.IndicatorAliasVo;
import com.rayk.health.indicator.vo.IndicatorDictVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/indicators")
public class IndicatorController {
    private final IndicatorDictService service;

    public IndicatorController(IndicatorDictService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<IndicatorDictVo>> list(
            @RequestParam(required = false) String category) {
        return ApiResponse.success(service.listIndicators(category));
    }

    @GetMapping("/{code}")
    public ApiResponse<IndicatorDictVo> get(@PathVariable String code) {
        return ApiResponse.success(service.getIndicator(code));
    }

    @PostMapping
    public ApiResponse<IndicatorDictVo> create(@Valid @RequestBody CreateIndicatorRequest request) {
        return ApiResponse.success(service.createIndicator(request));
    }

    @PutMapping("/{code}")
    public ApiResponse<IndicatorDictVo> update(
            @PathVariable String code, @Valid @RequestBody UpdateIndicatorRequest request) {
        return ApiResponse.success(service.updateIndicator(code, request));
    }

    @GetMapping("/{code}/aliases")
    public ApiResponse<List<IndicatorAliasVo>> listAliases(@PathVariable String code) {
        return ApiResponse.success(service.listAliases(code));
    }

    @PostMapping("/{code}/aliases")
    public ApiResponse<IndicatorAliasVo> addAlias(
            @PathVariable String code, @Valid @RequestBody AddAliasRequest request) {
        return ApiResponse.success(service.addAlias(code, request));
    }
}
