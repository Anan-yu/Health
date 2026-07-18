package com.rayk.health.workbench.controller;

import com.rayk.health.common.api.ApiResponse;
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

    @GetMapping("/summary")
    public ApiResponse<HomeSummaryData> summary() {
        CurrentPrincipal current = CurrentUser.require();
        boolean customer = "CUSTOMER".equals(current.workbench());
        List<HomeMetric> metrics =
                customer
                        ? List.of(
                                new HomeMetric("PROFILE", "档案完整度", 80, "/pages-customer/profile/index"),
                                new HomeMetric(
                                        "REPORT", "已发布健康报告", 1, "/pages-customer/health-report/index"),
                                new HomeMetric(
                                        "FOLLOWUP", "待完成随访", 1, "/pages-customer/followup/index"))
                        : List.of(
                                new HomeMetric("PATIENT", "机构客户总量", 1, "/pages-business/patient/index"),
                                new HomeMetric("OCR", "待OCR校对", 1, "/pages-business/indicator/index"),
                                new HomeMetric("REVIEW", "待医生审核", 1, "/pages-business/review/index"),
                                new HomeMetric("FOLLOWUP", "待随访任务", 1, "/pages-business/followup/index"));
        return ApiResponse.success(
                new HomeSummaryData(current.workbench(), "欢迎使用 RayK A1", metrics, DISCLAIMER));
    }
}

