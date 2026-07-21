package com.rayk.health.followup.controller;

import com.rayk.health.assessment.application.WorkflowApplicationService;
import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.followup.dto.CreateFollowupRequest;
import com.rayk.health.followup.dto.FollowupFeedbackRequest;
import com.rayk.health.followup.vo.FollowupTaskVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/followups")
public class FollowupController {
    private final WorkflowApplicationService service;

    public FollowupController(WorkflowApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<FollowupTaskVo>> list() {
        return ApiResponse.success(PageResponse.of(service.listFollowups()));
    }

    @PostMapping
    public ApiResponse<FollowupTaskVo> create(@Valid @RequestBody CreateFollowupRequest request) {
        return ApiResponse.success(service.createFollowup(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<FollowupTaskVo> get(@PathVariable long id) {
        return ApiResponse.success(service.getFollowup(id));
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<FollowupTaskVo> complete(@PathVariable long id) {
        return ApiResponse.success(service.completeFollowup(id));
    }

    @PostMapping("/{id}/feedback")
    public ApiResponse<FollowupTaskVo> feedback(
            @PathVariable long id, @Valid @RequestBody FollowupFeedbackRequest request) {
        return ApiResponse.success(service.feedback(id, request.feedback()));
    }
}
