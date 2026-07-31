package com.rayk.health.healthscan.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.healthscan.application.HealthScanService;
import com.rayk.health.healthscan.vo.HealthScanResultVo;
import com.rayk.health.healthscan.vo.HealthScanSessionVo;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/me/health-scans")
public class HealthScanController {
    private final HealthScanService service;

    public HealthScanController(HealthScanService service) {
        this.service = service;
    }

    @PostMapping("/session")
    public ApiResponse<HealthScanSessionVo> createSession() {
        return ApiResponse.success(service.createSession());
    }

    @PostMapping(value = "/{id}/video", consumes = "multipart/form-data")
    public ApiResponse<HealthScanResultVo> upload(
            @PathVariable long id, @RequestPart MultipartFile video) {
        return ApiResponse.success(service.upload(id, video));
    }

    @GetMapping
    public ApiResponse<List<HealthScanResultVo>> listMine() {
        return ApiResponse.success(service.listMine());
    }

    @GetMapping("/{id}")
    public ApiResponse<HealthScanResultVo> get(@PathVariable long id) {
        return ApiResponse.success(service.get(id));
    }
}

