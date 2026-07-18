package com.rayk.health.laboratory.controller;

import com.rayk.health.assessment.application.WorkflowApplicationService;
import com.rayk.health.assessment.vo.AssessmentVo;
import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.laboratory.dto.ConfirmIndicatorsRequest;
import com.rayk.health.laboratory.dto.CreateLabReportRequest;
import com.rayk.health.laboratory.vo.LabReportVo;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/lab-reports")
public class LabReportController {
    private static final Set<String> ALLOWED_MIME =
            Set.of("application/pdf", "image/jpeg", "image/png");
    private static final long MAX_BYTES = 20L * 1024 * 1024;
    private final WorkflowApplicationService service;

    public LabReportController(WorkflowApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<LabReportVo> create(@Valid @RequestBody CreateLabReportRequest request) {
        return ApiResponse.success(service.createLabReport(request));
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ApiResponse<LabReportVo> simulatedUpload(
            @RequestParam long patientId, @RequestPart MultipartFile file) {
        validateFile(file);
        CreateLabReportRequest request =
                new CreateLabReportRequest(
                        patientId, file.getOriginalFilename(), LocalDate.now(), "SIMULATED_UPLOAD");
        return ApiResponse.success(service.createLabReport(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<LabReportVo>> list() {
        return ApiResponse.success(PageResponse.of(service.listLabReports()));
    }

    @GetMapping("/{id}")
    public ApiResponse<LabReportVo> get(@PathVariable long id) {
        return ApiResponse.success(service.getLabReport(id));
    }

    @PutMapping("/{id}/indicators")
    public ApiResponse<LabReportVo> replaceIndicators(
            @PathVariable long id, @Valid @RequestBody ConfirmIndicatorsRequest request) {
        return ApiResponse.success(service.replaceIndicators(id, request));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<LabReportVo> confirm(@PathVariable long id) {
        return ApiResponse.success(service.confirmIndicators(id));
    }

    @PostMapping("/{id}/submit-ai")
    public ApiResponse<AssessmentVo> submitAi(@PathVariable long id) {
        return ApiResponse.success(service.submitAi(id));
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lower = name.toLowerCase(Locale.ROOT);
        boolean allowedExtension =
                lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
        if (file.isEmpty()
                || file.getSize() > MAX_BYTES
                || !ALLOWED_MIME.contains(contentType)
                || !allowedExtension) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
    }
}

