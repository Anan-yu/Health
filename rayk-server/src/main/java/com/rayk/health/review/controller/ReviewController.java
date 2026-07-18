package com.rayk.health.review.controller;

import com.rayk.health.assessment.application.WorkflowApplicationService;
import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.report.vo.HealthReportVo;
import com.rayk.health.review.dto.ReviewDecisionRequest;
import com.rayk.health.review.vo.ReviewTaskVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews/tasks")
public class ReviewController {
    private final WorkflowApplicationService service;

    public ReviewController(WorkflowApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReviewTaskVo>> list() {
        return ApiResponse.success(PageResponse.of(service.listReviews()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReviewTaskVo> get(@PathVariable long id) {
        return ApiResponse.success(service.getReview(id));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<ReviewTaskVo> approve(
            @PathVariable long id, @Valid @RequestBody ReviewDecisionRequest request) {
        return ApiResponse.success(service.approve(id, request.opinion()));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ReviewTaskVo> reject(
            @PathVariable long id, @Valid @RequestBody ReviewDecisionRequest request) {
        return ApiResponse.success(service.reject(id, request.opinion()));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<HealthReportVo> publish(@PathVariable long id) {
        return ApiResponse.success(service.publish(id));
    }
}

