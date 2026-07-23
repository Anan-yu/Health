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
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
public class HomeController {
    private final PatientApplicationService patientService;
    private final HealthProfileService profileService;
    private final WorkflowApplicationService workflowService;
    private final PlatformOverviewService platformOverviewService;

    public HomeController(PatientApplicationService patientService, HealthProfileService profileService,
            WorkflowApplicationService workflowService, PlatformOverviewService platformOverviewService) {
        this.patientService = patientService;
        this.profileService = profileService;
        this.workflowService = workflowService;
        this.platformOverviewService = platformOverviewService;
    }

    @GetMapping("/summary")
    public ApiResponse<HomeSummaryData> summary() {
        CurrentPrincipal current = CurrentUser.require();
        List<HomeMetric> metrics = switch (current.workbench()) {
            case "CUSTOMER" -> customerMetrics();
            case "PLATFORM_ADMIN" -> platformMetrics();
            default -> doctorMetrics();
        };
        return ApiResponse.success(new HomeSummaryData(current.workbench(), "欢迎使用 Rayk AI", metrics, null));
    }

    private List<HomeMetric> platformMetrics() {
        PlatformOverviewVo overview = platformOverviewService.overview();
        return List.of(
                new HomeMetric("TENANT", "合作医院", overview.tenantCount(), "/pages-tenant/dashboard/index"),
                new HomeMetric("USER", "预录入医生", overview.userCount(), "/pages-tenant/dashboard/index"),
                new HomeMetric("FOLLOWUP", "当日随访", overview.todayFollowupCount(), "/pages-tenant/dashboard/followup"));
    }

    private List<HomeMetric> customerMetrics() {
        List<PatientVo> patients = patientService.list(null);
        long completeness = patients.isEmpty() ? 0
                : profileService.getProfile(Long.parseLong(patients.getFirst().id())).profileCompleteness();
        long reports = workflowService.listHealthReports().size();
        long followups = workflowService.listFollowups().stream().filter(item -> !"COMPLETED".equals(item.status())).count();
        return List.of(
                new HomeMetric("PROFILE", "档案完整度", completeness, "/pages-customer/profile/index"),
                new HomeMetric("REPORT", "已生成健康报告", reports, "/pages-customer/health-report/index"),
                new HomeMetric("FOLLOWUP", "待完成健康随访", followups, "/pages-customer/followup/index"));
    }

    private List<HomeMetric> doctorMetrics() {
        LocalDate today = LocalDate.now();
        long patients = workflowService.listLabReports().stream()
                .filter(item -> item.createdAt() != null && today.equals(item.createdAt().toLocalDate()))
                .map(item -> item.patientId())
                .distinct()
                .count();
        long assessments = workflowService.listAssessments().stream()
                .filter(item -> "SUCCESS".equals(item.status()))
                .filter(item -> item.createdAt() != null && today.equals(item.createdAt().toLocalDate()))
                .count();
        long reports = workflowService.listHealthReports().stream()
                .filter(item -> item.publishedAt() != null && today.equals(item.publishedAt().toLocalDate()))
                .count();
        return List.of(
                new HomeMetric("PATIENT", "已体检者数量", patients, "/pages-business/patient/index"),
                new HomeMetric("ASSESSMENT", "可查看 AI 评估", assessments, "/pages-business/assessment/index"),
                new HomeMetric("REPORT", "健康报告", reports, "/pages-business/lab-report/index"));
    }
}
