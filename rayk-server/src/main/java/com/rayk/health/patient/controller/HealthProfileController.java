package com.rayk.health.patient.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.patient.application.HealthProfileService;
import com.rayk.health.patient.dto.UpdateProfileRequest;
import com.rayk.health.patient.vo.HealthProfileVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/profile")
public class HealthProfileController {
    private final HealthProfileService service;

    public HealthProfileController(HealthProfileService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<HealthProfileVo> getProfile(@PathVariable long patientId) {
        return ApiResponse.success(service.getProfile(patientId));
    }

    @PutMapping
    public ApiResponse<HealthProfileVo> updateProfile(
            @PathVariable long patientId, @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(service.updateProfile(patientId, request));
    }
}
