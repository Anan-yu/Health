package com.rayk.health.indicator.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.indicator.application.AssessmentModelService;
import com.rayk.health.indicator.dto.CreateModelRequest;
import com.rayk.health.indicator.vo.HealthModelConfigVo;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assessment-models")
public class AssessmentModelController {
    private final AssessmentModelService service;

    public AssessmentModelController(AssessmentModelService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<HealthModelConfigVo>> listActive() {
        return ApiResponse.success(service.listActiveModels());
    }

    @GetMapping("/{modelCode}")
    public ApiResponse<HealthModelConfigVo> get(@PathVariable String modelCode) {
        return ApiResponse.success(service.getModel(modelCode));
    }

    @PostMapping
    public ApiResponse<HealthModelConfigVo> createVersion(
            @Valid @RequestBody CreateModelRequest request) {
        return ApiResponse.success(service.createModelVersion(request));
    }

    @PostMapping("/{modelCode}/activate")
    public ApiResponse<HealthModelConfigVo> activate(
            @PathVariable String modelCode, @RequestBody Map<String, String> body) {
        String version = body.get("version");
        return ApiResponse.success(service.activateModel(modelCode, version));
    }
}
