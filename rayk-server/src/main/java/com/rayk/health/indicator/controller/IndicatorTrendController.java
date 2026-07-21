package com.rayk.health.indicator.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.indicator.application.IndicatorTrendService;
import com.rayk.health.indicator.vo.TrendPointVo;
import com.rayk.health.indicator.vo.TrendSummaryVo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/indicators/{code}")
public class IndicatorTrendController {
    private final IndicatorTrendService trendService;

    public IndicatorTrendController(IndicatorTrendService trendService) {
        this.trendService = trendService;
    }

    @GetMapping("/trend")
    public ApiResponse<List<TrendPointVo>> trend(
            @PathVariable long patientId,
            @PathVariable String code,
            @RequestParam(defaultValue = "6") int months) {
        return ApiResponse.success(trendService.getTrend(patientId, code, months));
    }

    @GetMapping("/summary")
    public ApiResponse<TrendSummaryVo> summary(
            @PathVariable long patientId,
            @PathVariable String code) {
        return ApiResponse.success(trendService.getTrendSummary(patientId, code));
    }
}
