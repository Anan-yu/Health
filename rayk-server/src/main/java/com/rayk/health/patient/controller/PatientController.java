package com.rayk.health.patient.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.patient.application.PatientApplicationService;
import com.rayk.health.patient.dto.CreatePatientRequest;
import com.rayk.health.patient.vo.PatientVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {
    private final PatientApplicationService service;

    public PatientController(PatientApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<PatientVo>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(PageResponse.of(service.list(keyword)));
    }

    @PostMapping
    public ApiResponse<PatientVo> create(@Valid @RequestBody CreatePatientRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<PatientVo> get(@PathVariable long id) {
        return ApiResponse.success(service.get(id));
    }
}

