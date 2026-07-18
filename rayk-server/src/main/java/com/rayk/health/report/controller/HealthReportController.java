package com.rayk.health.report.controller;

import com.rayk.health.assessment.application.WorkflowApplicationService;
import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.report.vo.HealthReportVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health-reports")
public class HealthReportController {
    private final WorkflowApplicationService service;

    public HealthReportController(WorkflowApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<HealthReportVo>> list() {
        return ApiResponse.success(PageResponse.of(service.listHealthReports()));
    }

    @GetMapping("/{id}")
    public ApiResponse<HealthReportVo> get(@PathVariable long id) {
        return ApiResponse.success(service.getHealthReport(id));
    }
}

