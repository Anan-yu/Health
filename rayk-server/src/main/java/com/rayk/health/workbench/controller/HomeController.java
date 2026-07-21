package com.rayk.health.workbench.controller;

import com.rayk.health.assessment.application.WorkflowApplicationService;
import com.rayk.health.common.api.ApiResponse;
import com.rayk.health.patient.application.HealthProfileService;
import com.rayk.health.patient.application.PatientApplicationService;
import com.rayk.health.patient.vo.PatientVo;
import com.rayk.health.platform.application.PlatformOverviewService;
import com.rayk.health.platform.vo.PlatformOverviewVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.workbench.dto.HomeMetric;
import com.rayk.health.workbench.dto.HomeSummaryData;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
public class HomeController {
    private static final String DISCLAIMER =
            "该结果仅用于系统开发测试和健康管理参考，不构成医学诊断。";
    private final PatientApplicationService patientService;
    private final HealthProfileService profileService;
    private final WorkflowApplicationService workflowService;
    private final PlatformOverviewService platformOverviewService;

    public HomeController(
            PatientApplicationService patientService,
            HealthProfileService profileService,
            WorkflowApplicationService workflowService,
            PlatformOverviewService platformOverviewService) {
        this.patientService = patientService;
        this.profileService = profileService;
        this.workflowService = workflowService;
        this.platformOverviewService = platformOverviewService;
    }

    @GetMapping("/summary")
    public ApiResponse<HomeSummaryData> summary() {
        CurrentPrincipal current = CurrentUser.require();
        List<HomeMetric> metrics =
                switch (current.workbench()) {
                    case "CUSTOMER" -> customerMetrics();
                    case "PLATFORM_ADMIN" -> platformMetrics();
                    default -> businessMetrics(current);
                };
        return ApiResponse.success(
                new HomeSummaryData(current.workbench(), "欢迎使用 RayK A1", metrics, DISCLAIMER));
    }

    private List<HomeMetric> platformMetrics() {
        PlatformOverviewVo overview = platformOverviewService.overview();
        return List.of(
                new HomeMetric(
                        "TENANT",
                        "入驻机构",
                        overview.tenantCount(),
                        "/pages-tenant/dashboard/index"),
                new HomeMetric(
                        "USER",
                        "机构用户",
                        overview.userCount(),
                        "/pages-tenant/dashboard/index"),
                new HomeMetric(
                        "REVIEW",
                        "全平台待审核",
                        overview.pendingReviewCount(),
                        "/pages-tenant/dashboard/index"),
                new HomeMetric(
                        "FOLLOWUP",
                        "全平台待随访",
                        overview.pendingFollowupCount(),
                        "/pages-tenant/dashboard/index"));
    }

    private List<HomeMetric> customerMetrics() {
        List<PatientVo> patients = patientService.list();
        long completeness = patients.isEmpty()
                ? 0
                : profileService.getProfile(Long.parseLong(patients.getFirst().id())).profileCompleteness();
        long publishedReports = workflowService.listHealthReports().size();
        long pendingFollowups = workflowService.listFollowups().stream()
                .filter(item -> !"COMPLETED".equals(item.status()))
                .count();
        return List.of(
                new HomeMetric("PROFILE", "档案完整度", completeness, "/pages-customer/profile/index"),
                new HomeMetric("REPORT", "已发布健康报告", publishedReports, "/pages-customer/health-report/index"),
                new HomeMetric("FOLLOWUP", "待完成随访", pendingFollowups, "/pages-customer/followup/index"));
    }

    private List<HomeMetric> businessMetrics(CurrentPrincipal current) {
        long patients = patientService.list().size();
        long waitingConfirmation = workflowService.listLabReports().stream()
                .filter(item -> "WAITING_CONFIRMATION".equals(item.status()))
                .count();
        long waitingFollowups = workflowService.listFollowups().stream()
                .filter(item -> !"COMPLETED".equals(item.status()))
                .count();
        if (current.permissions().contains("assessment:review")) {
            long waitingReviews = workflowService.listReviews().stream()
                    .filter(item -> "WAITING_REVIEW".equals(item.status()))
                    .count();
            return List.of(
                    new HomeMetric("PATIENT", "机构客户总量", patients, "/pages-business/patient/index"),
                    new HomeMetric("OCR", "待OCR校对", waitingConfirmation, "/pages-business/indicator/index"),
                    new HomeMetric("REVIEW", "待医生审核", waitingReviews, "/pages-business/review/index"),
                    new HomeMetric("FOLLOWUP", "待随访任务", waitingFollowups, "/pages-business/followup/index"));
        }
        long assessments = workflowService.listAssessments().stream()
                .filter(item -> "SUCCESS".equals(item.status()))
                .count();
        String patientLabel =
                "TENANT_ADMIN".equals(current.workbench()) ? "机构客户总量" : "我的客户总量";
        return List.of(
                new HomeMetric("PATIENT", patientLabel, patients, "/pages-business/patient/index"),
                new HomeMetric("OCR", "待OCR校对", waitingConfirmation, "/pages-business/indicator/index"),
                new HomeMetric("ASSESSMENT", "已完成评估", assessments, "/pages-business/assessment/index"),
                new HomeMetric("FOLLOWUP", "待随访任务", waitingFollowups, "/pages-business/followup/index"));
    }
}
