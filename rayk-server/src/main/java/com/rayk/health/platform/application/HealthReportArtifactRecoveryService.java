package com.rayk.health.platform.application;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.platform.dto.HealthReportArtifactRecoveryData;
import com.rayk.health.platform.mapper.HealthReportRecoveryMapper;
import com.rayk.health.report.application.PdfReportService;
import com.rayk.health.report.application.PdfReportService.ArtifactRecoveryResult;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.system.aspect.Audited;
import com.rayk.health.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HealthReportArtifactRecoveryService {
    private final HealthReportRecoveryMapper recoveryMapper;
    private final PdfReportService pdfReportService;

    public HealthReportArtifactRecoveryService(
            HealthReportRecoveryMapper recoveryMapper, PdfReportService pdfReportService) {
        this.recoveryMapper = recoveryMapper;
        this.pdfReportService = pdfReportService;
    }

    @Audited(
            operationType = "RECOVER_HEALTH_REPORT_ARTIFACTS",
            resourceType = "HEALTH_REPORT")
    public HealthReportArtifactRecoveryData recoverMissingArtifacts() {
        CurrentPrincipal current = CurrentUser.require();
        List<HealthReportArtifactRecoveryData.Item> items = new ArrayList<>();
        int recovered = 0;
        int skipped = 0;
        for (HealthReportRecoveryMapper.PublishedReportRef ref :
                recoveryMapper.selectPublishedReports()) {
            Long previousTenant = TenantContext.get();
            ArtifactRecoveryResult result;
            try {
                TenantContext.set(ref.tenantId());
                result =
                        pdfReportService.recoverMissingPublishedArtifact(
                                ref.reportId(), current.userId());
            } finally {
                if (previousTenant == null) {
                    TenantContext.clear();
                } else {
                    TenantContext.set(previousTenant);
                }
            }
            if ("RECOVERED".equals(result.status())) {
                recovered++;
            } else {
                skipped++;
            }
            items.add(
                    new HealthReportArtifactRecoveryData.Item(
                            String.valueOf(result.reportId()),
                            String.valueOf(ref.tenantId()),
                            result.status(),
                            result.versionNo(),
                            result.objectPath()));
        }
        return new HealthReportArtifactRecoveryData(items.size(), recovered, skipped, List.copyOf(items));
    }

    @Audited(
            operationType = "REGENERATE_HEALTH_REPORT_ARTIFACT",
            resourceType = "HEALTH_REPORT")
    public ArtifactRecoveryResult regenerateHealthReportArtifact(long reportId) {
        CurrentPrincipal current = CurrentUser.require();
        HealthReportRecoveryMapper.PublishedReportRef ref =
                recoveryMapper.selectPublishedReports().stream()
                        .filter(item -> item.reportId() == reportId)
                        .findFirst()
                        .orElseThrow(
                                () -> new BusinessException(ErrorCode.LAB_REPORT_NOT_FOUND));
        Long previousTenant = TenantContext.get();
        try {
            TenantContext.set(ref.tenantId());
            return pdfReportService.regeneratePublishedArtifact(reportId, current.userId());
        } finally {
            if (previousTenant == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(previousTenant);
            }
        }
    }
}
