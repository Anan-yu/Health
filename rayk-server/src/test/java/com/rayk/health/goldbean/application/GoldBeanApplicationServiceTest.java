package com.rayk.health.goldbean.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanLedgerMapper;
import com.rayk.health.goldbean.mapper.GoldBeanOrderMapper;
import com.rayk.health.goldbean.mapper.GoldBeanReferralMapper;
import com.rayk.health.goldbean.mapper.GoldRegionMapper;
import com.rayk.health.tenant.TenantContext;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class GoldBeanApplicationServiceTest {
    @Test
    void initializesCustomerWithExplicitTenantBeforeAuthentication() {
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        GoldBeanApplicationService service = new GoldBeanApplicationService(
                new GoldBeanProperties(true, true, 60, 60, 20, 100, 99_800, 7, 50,
                        true, "normal_member_998", "gold_bean", 100, 10_000,
                        "机器人权益服务群", ""),
                accountMapper,
                mock(GoldBeanReferralMapper.class),
                mock(GoldBeanLedgerMapper.class),
                mock(GoldBeanOrderMapper.class),
                mock(GoldRegionMapper.class),
                mock(GoldBeanPlatformInviteService.class));
        AtomicReference<Long> observedTenant = new AtomicReference<>();

        doAnswer(invocation -> {
            observedTenant.set(TenantContext.get());
            return null;
        }).when(accountMapper).selectOne(any());
        doAnswer(invocation -> {
            assertThat(TenantContext.get()).isEqualTo(42L);
            return 1;
        }).when(accountMapper).insert(any(GoldBeanAccountEntity.class));

        TenantContext.clear();
        try {
            service.initializeForCustomer(42L, 7L);

            assertThat(observedTenant).hasValue(42L);
            assertThat(TenantContext.get()).isNull();
            verify(accountMapper).insert(any(GoldBeanAccountEntity.class));
        } finally {
            TenantContext.clear();
        }
    }
}
