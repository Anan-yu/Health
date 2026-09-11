package com.rayk.health.goldbean.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanReferralAuthorizationEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanReferralAuthorizationMapper;
import com.rayk.health.goldbean.vo.GoldBeanReferralAuthorizationVo;
import com.rayk.health.membership.config.MembershipProperties;
import com.rayk.health.membership.payment.WeChatMerchantTransferClient;
import com.rayk.health.membership.payment.WeChatPayClient;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.wechat.entity.WeChatUserBindingEntity;
import com.rayk.health.security.wechat.mapper.WeChatUserBindingMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

class GoldBeanReferralAuthorizationServiceTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void keepsTheCreatePackageWhenAStatusQueryOmitsPackageInfo() {
        GoldBeanReferralAuthorizationMapper authorizationMapper = mock(GoldBeanReferralAuthorizationMapper.class);
        GoldBeanReferralAuthorizationService service = service(authorizationMapper);
        GoldBeanReferralAuthorizationEntity authorization = authorization(WAIT_USER_CONFIRM, "auth-package");

        service.applyResponse(
                authorization,
                new WeChatMerchantTransferClient.TransferAuthorization(
                        "GRA-1", "", GoldBeanReferralAuthorizationService.WAIT_USER_CONFIRM, "", ""));

        assertThat(authorization.getState()).isEqualTo(GoldBeanReferralAuthorizationService.WAIT_USER_CONFIRM);
        assertThat(authorization.getPackageInfo()).isEqualTo("auth-package");
        verify(authorizationMapper).updateById(authorization);
    }

    @Test
    void recreatesAUsableAuthorizationWhenAnOlderRecordLostItsPackage() {
        GoldBeanReferralAuthorizationMapper authorizationMapper = mock(GoldBeanReferralAuthorizationMapper.class);
        ServiceDependencies dependencies = serviceDependencies();
        GoldBeanReferralAuthorizationEntity stale = authorization(WAIT_USER_CONFIRM, null);
        stale.setOutAuthorizationNo("GRA-OLD");
        GoldBeanAccountEntity account = new GoldBeanAccountEntity();
        account.setTenantId(1L);
        account.setUserId(2L);
        account.setStatus("ACTIVE");
        account.setRegistrationFeeStatus("PAID");
        WeChatUserBindingEntity binding = new WeChatUserBindingEntity();
        binding.setTenantId(1L);
        binding.setUserId(2L);
        binding.setAppId("app-id");
        binding.setOpenid("openid");
        binding.setStatus("ACTIVE");
        binding.setDeleted(0);

        when(authorizationMapper.selectOne(any())).thenReturn(null, stale);
        when(authorizationMapper.insert(any(GoldBeanReferralAuthorizationEntity.class))).thenReturn(1);

        // Rebuild the service with the mocks needed by the public recovery path.
        GoldBeanAccountMapper accountMapper = dependencies.accountMapper;
        WeChatUserBindingMapper bindingMapper = dependencies.bindingMapper;
        WeChatMerchantTransferClient merchantTransferClient = dependencies.merchantTransferClient;
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(bindingMapper.selectOne(any())).thenReturn(binding);
        when(merchantTransferClient.queryAuthorization("GRA-OLD"))
                .thenReturn(new WeChatMerchantTransferClient.TransferAuthorization(
                        "GRA-OLD", "", WAIT_USER_CONFIRM, "", ""));
        when(merchantTransferClient.initiateAuthorization(
                        anyString(),
                        eq("openid"),
                        eq("1005"),
                        eq("三羊健康会员"),
                        eq("劳务报酬"),
                        eq("https://example.test/authorization-notify")))
                .thenReturn(new WeChatMerchantTransferClient.TransferAuthorization(
                        "GRA-NEW", "", WAIT_USER_CONFIRM, "new-auth-package", ""));

        // This test uses a service assembled from the same dependencies so the stale record is
        // queried first and a new package is returned when the old one is unusable.
        GoldBeanReferralAuthorizationService service = newService(
                accountMapper,
                authorizationMapper,
                bindingMapper,
                merchantTransferClient);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "user", 2L, 1L, List.of("CUSTOMER"), List.of("self:health-record"), "CUSTOMER"),
                null));

        GoldBeanReferralAuthorizationVo result = service.beginAuthorization();

        assertThat(result.userConfirmationRequired()).isTrue();
        assertThat(result.packageInfo()).isEqualTo("new-auth-package");
        verify(merchantTransferClient).queryAuthorization("GRA-OLD");
        verify(merchantTransferClient).initiateAuthorization(
                anyString(),
                eq("openid"),
                eq("1005"),
                eq("三羊健康会员"),
                eq("劳务报酬"),
                eq("https://example.test/authorization-notify"));
    }

    private GoldBeanReferralAuthorizationService service(
            GoldBeanReferralAuthorizationMapper authorizationMapper) {
        ServiceDependencies dependencies = serviceDependencies();
        return newService(
                dependencies.accountMapper,
                authorizationMapper,
                dependencies.bindingMapper,
                dependencies.merchantTransferClient);
    }

    private GoldBeanReferralAuthorizationService newService(
            GoldBeanAccountMapper accountMapper,
            GoldBeanReferralAuthorizationMapper authorizationMapper,
            WeChatUserBindingMapper bindingMapper,
            WeChatMerchantTransferClient merchantTransferClient) {
        GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);
        when(goldBeanService.available()).thenReturn(true);
        WeChatPayClient weChatPayClient = mock(WeChatPayClient.class);
        when(weChatPayClient.configured()).thenReturn(true);
        return new GoldBeanReferralAuthorizationService(
                new GoldBeanProperties(
                        true,
                        true,
                        60,
                        60,
                        20,
                        100,
                        99_800,
                        7,
                        50,
                        true,
                        "normal_member_998",
                        "gold_bean",
                        100,
                        10_000,
                        "机器人权益服务群",
                        ""),
                goldBeanService,
                accountMapper,
                authorizationMapper,
                bindingMapper,
                merchantTransferClient,
                weChatPayClient,
                new MembershipProperties(
                        true,
                        true,
                        3,
                        3,
                        3,
                        3,
                        3,
                        3,
                        false,
                        new MembershipProperties.WeChatPayProperties(
                                "app-id", "merchant-id", "serial", "private-key", "api-v3", "", "", "notify"),
                        MembershipProperties.WeChatVirtualPayProperties.empty()),
                mock(PlatformTransactionManager.class),
                true,
                "1005",
                "https://example.test/authorization-notify",
                "劳务报酬",
                30);
    }

    private ServiceDependencies serviceDependencies() {
        return new ServiceDependencies(
                mock(GoldBeanAccountMapper.class),
                mock(WeChatUserBindingMapper.class),
                mock(WeChatMerchantTransferClient.class));
    }

    private GoldBeanReferralAuthorizationEntity authorization(String state, String packageInfo) {
        GoldBeanReferralAuthorizationEntity authorization = new GoldBeanReferralAuthorizationEntity();
        authorization.setId(1L);
        authorization.setUserId(2L);
        authorization.setTenantId(1L);
        authorization.setAppId("app-id");
        authorization.setOpenid("openid");
        authorization.setTransferSceneId("1005");
        authorization.setOutAuthorizationNo("GRA-1");
        authorization.setState(state);
        authorization.setPackageInfo(packageInfo);
        authorization.setCreatedAt(LocalDateTime.now());
        authorization.setUpdatedAt(LocalDateTime.now());
        authorization.setDeleted(0);
        return authorization;
    }

    private static final String WAIT_USER_CONFIRM = "WAIT_USER_CONFIRM";

    private record ServiceDependencies(
            GoldBeanAccountMapper accountMapper,
            WeChatUserBindingMapper bindingMapper,
            WeChatMerchantTransferClient merchantTransferClient) {}
}
