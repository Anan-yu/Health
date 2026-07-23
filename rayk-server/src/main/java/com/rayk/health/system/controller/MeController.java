package com.rayk.health.system.controller;

import com.rayk.health.assessment.application.WorkflowApplicationService;
import com.rayk.health.assessment.vo.AssessmentVo;
import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.followup.dto.FollowupFeedbackRequest;
import com.rayk.health.followup.vo.FollowupTaskVo;
import com.rayk.health.laboratory.vo.LabReportVo;
import com.rayk.health.patient.application.PatientApplicationService;
import com.rayk.health.patient.vo.PatientVo;
import com.rayk.health.report.vo.HealthReportVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    private final PatientApplicationService patientService;
    private final WorkflowApplicationService workflowService;

    public MeController(
            PatientApplicationService patientService, WorkflowApplicationService workflowService) {
        this.patientService = patientService;
        this.workflowService = workflowService;
    }

    @GetMapping("/health-profile")
    public ApiResponse<PatientVo> profile() {
        return ApiResponse.success(patientService.list(null).stream().findFirst().orElse(null));
    }

    @GetMapping("/lab-reports")
    public ApiResponse<List<LabReportVo>> labReports() {
        return ApiResponse.success(workflowService.listLabReports());
    }

    @GetMapping("/lab-reports/{id}")
    public ApiResponse<LabReportVo> labReport(@PathVariable long id) {
        return ApiResponse.success(workflowService.getLabReport(id));
    }

    @GetMapping("/assessments")
    public ApiResponse<List<AssessmentVo>> assessments() {
        return ApiResponse.success(workflowService.listAssessments());
    }

    @GetMapping("/assessments/{id}")
    public ApiResponse<AssessmentVo> assessment(@PathVariable long id) {
        return ApiResponse.success(workflowService.getAssessment(id));
    }

    @GetMapping("/health-reports")
    public ApiResponse<List<HealthReportVo>> healthReports() {
        return ApiResponse.success(workflowService.listHealthReports());
    }

    @GetMapping("/health-reports/{id}")
    public ApiResponse<HealthReportVo> healthReport(@PathVariable long id) {
        return ApiResponse.success(workflowService.getHealthReport(id));
    }

    @GetMapping("/followups")
    public ApiResponse<List<FollowupTaskVo>> followups() {
        return ApiResponse.success(workflowService.listFollowups());
    }

    @PostMapping("/followups/{id}/feedback")
    public ApiResponse<FollowupTaskVo> feedback(
            @PathVariable long id, @Valid @RequestBody FollowupFeedbackRequest request) {
        return ApiResponse.success(workflowService.feedback(id, request.feedback()));
    }
}

