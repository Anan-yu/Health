package com.rayk.health.platform.controller;

import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.platform.application.AiModelRuntimeConfigService;
import com.rayk.health.platform.dto.SwitchAiModelRequest;
import com.rayk.health.platform.dto.SwitchAiThinkingRequest;
import com.rayk.health.platform.vo.AiModelRuntimeConfigVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/ai-models")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AiModelRuntimeConfigController {
    private final AiModelRuntimeConfigService service;

    public AiModelRuntimeConfigController(AiModelRuntimeConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AiModelRuntimeConfigVo>> list() {
        return ApiResponse.success(service.listModels());
    }

    @PutMapping("/{modelCode}")
    public ApiResponse<AiModelRuntimeConfigVo> switchModel(
        @PathVariable String modelCode, @Valid @RequestBody SwitchAiModelRequest request) {
        if (!modelCode.equals(request.modelCode())) {
            throw new BusinessException(ErrorCode.MODEL_CONFIG_INVALID_STATUS);
        }
        return ApiResponse.success(service.switchModel(modelCode));
    }

    @PutMapping("/thinking")
    public ApiResponse<AiModelRuntimeConfigVo> switchThinking(
            @Valid @RequestBody SwitchAiThinkingRequest request) {
        return ApiResponse.success(service.switchThinking(request.enabled()));
    }
}
