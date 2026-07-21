package com.rayk.health.platform.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rayk.health.platform.mapper.PlatformOverviewMapper;
import com.rayk.health.platform.vo.PlatformOverviewVo;
import com.rayk.health.platform.vo.TenantSummaryVo;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlatformOverviewServiceTest {
    @Test
    void aggregatesCrossTenantMetricsWithoutSensitiveDetails() {
        PlatformOverviewMapper mapper = mock(PlatformOverviewMapper.class);
        when(mapper.countTenants()).thenReturn(2L);
        when(mapper.countActiveTenants()).thenReturn(2L);
        when(mapper.countUsers()).thenReturn(7L);
        when(mapper.countPatients()).thenReturn(2L);
        when(mapper.countPendingReviews()).thenReturn(1L);
        when(mapper.countPendingFollowups()).thenReturn(3L);
        when(mapper.selectTenants())
                .thenReturn(
                        List.of(
                                new TenantSummaryVo(
                                        "20001",
                                        "RAYK_DEMO",
                                        "测试机构",
                                        "ACTIVE",
                                        "DEVELOPMENT",
                                        4L)));

        PlatformOverviewVo result = new PlatformOverviewService(mapper).overview();

        assertThat(result.tenantCount()).isEqualTo(2L);
        assertThat(result.activeTenantCount()).isEqualTo(2L);
        assertThat(result.userCount()).isEqualTo(7L);
        assertThat(result.patientCount()).isEqualTo(2L);
        assertThat(result.pendingReviewCount()).isEqualTo(1L);
        assertThat(result.pendingFollowupCount()).isEqualTo(3L);
        assertThat(result.tenants()).singleElement().extracting(TenantSummaryVo::id).isEqualTo("20001");
    }
}
