package com.rayk.health.assistant.controller;

import com.rayk.health.assistant.application.MedicalAssistantApplicationService;
import com.rayk.health.assistant.vo.HealthTreeHoleFeedbackVo;
import com.rayk.health.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/health-tree-hole")
@PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
public class HealthTreeHoleController {
    private final MedicalAssistantApplicationService service;

    public HealthTreeHoleController(MedicalAssistantApplicationService service) {
        this.service = service;
    }

    @GetMapping("/feedback")
    public ApiResponse<HealthTreeHoleFeedbackVo> feedback() {
        return ApiResponse.success(service.treeHoleFeedback());
    }

    @PostMapping("/feedback/generate")
    public ApiResponse<HealthTreeHoleFeedbackVo> generateFeedback() {
        return ApiResponse.success(service.generateTreeHoleFeedback());
    }
}
