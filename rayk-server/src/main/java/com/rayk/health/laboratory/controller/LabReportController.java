package com.rayk.health.laboratory.controller;

import com.rayk.health.assessment.application.WorkflowApplicationService;
import com.rayk.health.assessment.vo.AssessmentVo;
import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.laboratory.application.LabReportFileService;
import com.rayk.health.laboratory.dto.ConfirmIndicatorsRequest;
import com.rayk.health.laboratory.dto.CreateLabReportRequest;
import com.rayk.health.laboratory.vo.LabReportVo;
import com.rayk.health.laboratory.vo.LabReportFileVo;
import com.rayk.health.laboratory.vo.LabReportUploadVo;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final WorkflowApplicationService service;
    private final LabReportFileService fileService;

    public LabReportController(
            WorkflowApplicationService service, LabReportFileService fileService) {
        this.service = service;
        this.fileService = fileService;
    }

    @PostMapping
    public ApiResponse<LabReportVo> create(@Valid @RequestBody CreateLabReportRequest request) {
        return ApiResponse.success(service.createLabReport(request));
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ApiResponse<LabReportUploadVo> upload(
            @RequestParam long patientId,
            @RequestParam(required = false) String reportName,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate reportDate,
            @RequestPart MultipartFile file) {
        return ApiResponse.success(fileService.upload(patientId, reportName, reportDate, file));
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

    @GetMapping("/{id}/files")
    public ApiResponse<List<LabReportFileVo>> files(@PathVariable long id) {
        return ApiResponse.success(fileService.list(id));
    }

    @PostMapping("/{id}/files/{fileId}/download-url")
    public ApiResponse<LabReportFileVo> downloadUrl(
            @PathVariable long id, @PathVariable long fileId) {
        return ApiResponse.success(fileService.createDownloadUrl(id, fileId));
    }
}
