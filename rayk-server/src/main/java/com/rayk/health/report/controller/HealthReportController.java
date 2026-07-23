package com.rayk.health.report.controller;

import com.rayk.health.assessment.application.WorkflowApplicationService;
import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.report.application.PdfReportService;
import com.rayk.health.report.vo.HealthReportVo;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health-reports")
public class HealthReportController {
    private final WorkflowApplicationService service;
    private final PdfReportService pdfReportService;

    public HealthReportController(WorkflowApplicationService service, PdfReportService pdfReportService) {
        this.service = service;
        this.pdfReportService = pdfReportService;
    }

    @GetMapping
    public ApiResponse<PageResponse<HealthReportVo>> list(
            @RequestParam(required = false) Long patientId) {
        return ApiResponse.success(PageResponse.of(service.listHealthReports(patientId)));
    }

    @GetMapping("/{id}")
    public ApiResponse<HealthReportVo> get(@PathVariable long id) {
        return ApiResponse.success(service.getHealthReport(id));
    }

    @PostMapping("/recover/{assessmentId}")
    public ApiResponse<HealthReportVo> recover(@PathVariable long assessmentId) {
        return ApiResponse.success(service.recoverHealthReport(assessmentId));
    }

    @GetMapping("/{id}/download")
    public ApiResponse<Map<String, String>> download(@PathVariable long id) {
        String url = pdfReportService.getDownloadUrl(id);
        return ApiResponse.success(Map.of("downloadUrl", url));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> content(@PathVariable long id) {
        PdfReportService.DownloadedPdf pdf = pdfReportService.openDownload(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(pdf.filename(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(new InputStreamResource(pdf.inputStream()));
    }
}

