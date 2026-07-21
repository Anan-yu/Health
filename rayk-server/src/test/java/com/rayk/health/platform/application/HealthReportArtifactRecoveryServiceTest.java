package com.rayk.health.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rayk.health.platform.mapper.HealthReportRecoveryMapper;
import com.rayk.health.platform.mapper.HealthReportRecoveryMapper.PublishedReportRef;
import com.rayk.health.report.application.PdfReportService;
import com.rayk.health.report.application.PdfReportService.ArtifactRecoveryResult;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class HealthReportArtifactRecoveryServiceTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void recoversCrossTenantReportsAndRestoresTenantContext() {
        HealthReportRecoveryMapper mapper = mock(HealthReportRecoveryMapper.class);
        PdfReportService pdfReportService = mock(PdfReportService.class);
        when(mapper.selectPublishedReports())
                .thenReturn(List.of(new PublishedReportRef(11L, 20001L), new PublishedReportRef(12L, 20002L)));
        when(pdfReportService.recoverMissingPublishedArtifact(11L, 10001L))
                .thenReturn(new ArtifactRecoveryResult(11L, "RECOVERED", 2, "a/report-v02.pdf"));
        when(pdfReportService.recoverMissingPublishedArtifact(12L, 10001L))
                .thenReturn(new ArtifactRecoveryResult(12L, "SKIPPED", 1, "b/report.pdf"));
        CurrentPrincipal principal =
                new CurrentPrincipal(
                        "jti-test",
                        "platform_admin",
                        10001L,
                        1L,
                        List.of("PLATFORM_ADMIN"),
                        List.of("platform:tenant:list"),
                        "PLATFORM_ADMIN");
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(principal, null));

        var result =
                new HealthReportArtifactRecoveryService(mapper, pdfReportService)
                        .recoverMissingArtifacts();

        assertThat(result.scanned()).isEqualTo(2);
        assertThat(result.recovered()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.items()).hasSize(2);
        assertThat(TenantContext.get()).isNull();
    }
}
