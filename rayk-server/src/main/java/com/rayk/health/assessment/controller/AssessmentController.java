package com.rayk.health.assessment.controller;

import com.rayk.health.assessment.application.WorkflowApplicationService;
import com.rayk.health.assessment.dto.CreateAssessmentRequest;
import com.rayk.health.assessment.vo.AssessmentVo;
import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {
    private final WorkflowApplicationService service;

    public AssessmentController(WorkflowApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<AssessmentVo>> list() {
        return ApiResponse.success(PageResponse.of(service.listAssessments()));
    }

    @PostMapping
    public ApiResponse<AssessmentVo> create(@Valid @RequestBody CreateAssessmentRequest request) {
        return ApiResponse.success(service.submitAi(request.reportId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<AssessmentVo> get(@PathVariable long id) {
        return ApiResponse.success(service.getAssessment(id));
    }
}

