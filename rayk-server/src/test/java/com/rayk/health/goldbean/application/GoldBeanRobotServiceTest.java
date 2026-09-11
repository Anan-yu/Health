package com.rayk.health.goldbean.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.dto.RedeemGoldRobotRequest;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldRobotRedemptionEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldRobotRedemptionMapper;
import com.rayk.health.goldbean.vo.GoldRobotRedemptionVo;
import com.rayk.health.security.service.CurrentPrincipal;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class GoldBeanRobotServiceTest {
    private final GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
    private final GoldRobotRedemptionMapper redemptionMapper = mock(GoldRobotRedemptionMapper.class);
    private final GoldBeanApplicationService goldBeanService = mock(GoldBeanApplicationService.class);

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void reportsThatAnEligibleUserCanRedeemOnce() {
        GoldBeanAccountEntity account = account(10_000L);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(redemptionMapper.selectCount(any())).thenReturn(0L);
        authenticateCustomer();

        var status = service("https://cdn.example.test/robot-group.png").status();

        assertThat(status.canRedeem()).isTrue();
        assertThat(status.groupConfigured()).isTrue();
        assertThat(status.costGoldBeans()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
        assertThat(status.redeemedCount()).isZero();
    }

    @Test
    void reportsTheBundledGroupQrAsConfiguredWhenExternalConfigIsEmpty() {
        GoldBeanAccountEntity account = account(10_000L);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(redemptionMapper.selectCount(any())).thenReturn(0L);
        authenticateCustomer();

        var status = service("").status();

        assertThat(status.canRedeem()).isTrue();
        assertThat(status.groupConfigured()).isTrue();
    }

    @Test
    void keepsTheRedemptionActionAvailableWhenTheDigitalBankBalanceIsInsufficient() {
        GoldBeanAccountEntity account = account(122L);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(redemptionMapper.selectCount(any())).thenReturn(0L);
        authenticateCustomer();

        var status = service("https://cdn.example.test/robot-group.png").status();

        assertThat(status.canRedeem()).isTrue();
        assertThat(status.digitalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(122));
    }

    @Test
    void reportsThatAUserWithACompletedRedemptionCannotRedeemAgain() {
        GoldBeanAccountEntity account = account(20_000L);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(redemptionMapper.selectCount(any())).thenReturn(1L);
        authenticateCustomer();

        var status = service("https://cdn.example.test/robot-group.png").status();

        assertThat(status.canRedeem()).isFalse();
        assertThat(status.redeemedCount()).isEqualTo(1L);
    }

    @Test
    void rejectsRedemptionWithoutEnoughDigitalBankBeans() {
        GoldBeanAccountEntity account = account(9_999L);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(redemptionMapper.selectOne(any())).thenReturn(null);
        authenticateCustomer();

        assertThatThrownBy(() -> service("https://cdn.example.test/robot-group.png")
                        .redeem(new RedeemGoldRobotRequest("robot-request-1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("数字银行金豆余额不足");

        verify(goldBeanService, never())
                .debitDigitalBankForRobot(any(), any(BigDecimal.class), anyLong(), any(), any());
        verify(redemptionMapper, never()).insert(any(GoldRobotRedemptionEntity.class));
    }

    @Test
    void debitsExactlyTenThousandAndReturnsEnterpriseWechatGroupQr() {
        GoldBeanAccountEntity account = account(12_345L);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(redemptionMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
                    account.setDigitalBankBalance(account.getDigitalBankBalance()
                            .subtract(invocation.getArgument(1, BigDecimal.class)));
                    return null;
                })
                .when(goldBeanService)
                .debitDigitalBankForRobot(any(), any(BigDecimal.class), anyLong(), any(), any());
        authenticateCustomer();

        GoldRobotRedemptionVo result = service("https://cdn.example.test/robot-group.png")
                .redeem(new RedeemGoldRobotRequest("robot-request-2"));

        assertThat(result.entitlementCode()).isEqualTo("ROBOT");
        assertThat(result.entitlementName()).isEqualTo("机器人权益");
        assertThat(result.goldBeanCost()).isEqualByComparingTo(BigDecimal.valueOf(10_000));
        assertThat(result.digitalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(2_345));
        assertThat(result.groupName()).isEqualTo("机器人权益服务群");
        assertThat(result.groupQrImageUrl()).isEqualTo("https://cdn.example.test/robot-group.png");
        verify(goldBeanService)
                .debitDigitalBankForRobot(
                        eq(account), eq(BigDecimal.valueOf(10_000)), eq(3L), any(), eq("兑换机器人权益"));
        verify(redemptionMapper).insert(any(GoldRobotRedemptionEntity.class));
    }

    @Test
    void usesTheBundledGroupQrWhenExternalConfigIsEmpty() {
        GoldBeanAccountEntity account = account(10_000L);
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(redemptionMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
                    account.setDigitalBankBalance(account.getDigitalBankBalance()
                            .subtract(invocation.getArgument(1, BigDecimal.class)));
                    return null;
                })
                .when(goldBeanService)
                .debitDigitalBankForRobot(any(), any(BigDecimal.class), anyLong(), any(), any());
        authenticateCustomer();

        GoldRobotRedemptionVo result = service("")
                .redeem(new RedeemGoldRobotRequest("robot-request-3"));

        assertThat(result.groupQrImageUrl())
                .isEqualTo(GoldBeanRobotService.BUNDLED_GROUP_QR_IMAGE_URL);
        assertThat(result.digitalBankBalance()).isZero();
        verify(goldBeanService)
                .debitDigitalBankForRobot(
                        eq(account), eq(BigDecimal.valueOf(10_000)), eq(3L), any(), eq("兑换机器人权益"));
        verify(redemptionMapper).insert(any(GoldRobotRedemptionEntity.class));
    }

    @Test
    void returnsTheExistingRedemptionForAnIdempotentRetry() {
        GoldBeanAccountEntity account = account(2_345L);
        GoldRobotRedemptionEntity existing = new GoldRobotRedemptionEntity();
        existing.setId(99L);
        existing.setRedemptionNo("GBRBT99");
        existing.setEntitlementCode("ROBOT");
        existing.setGoldBeanCost(BigDecimal.valueOf(10_000));
        existing.setGroupName("机器人权益服务群");
        existing.setGroupQrImageUrl("https://cdn.example.test/robot-group.png");
        existing.setRedeemedAt(java.time.LocalDateTime.now());
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(redemptionMapper.selectOne(any())).thenReturn(existing);
        authenticateCustomer();

        GoldRobotRedemptionVo result = service("https://cdn.example.test/rotated.png")
                .redeem(new RedeemGoldRobotRequest("robot-request-4"));

        assertThat(result.redemptionNo()).isEqualTo("GBRBT99");
        assertThat(result.digitalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(2_345));
        assertThat(result.groupQrImageUrl()).isEqualTo("https://cdn.example.test/robot-group.png");
        verify(goldBeanService, never())
                .debitDigitalBankForRobot(any(), any(BigDecimal.class), anyLong(), any(), any());
        verify(redemptionMapper, never()).insert(any(GoldRobotRedemptionEntity.class));
    }

    @Test
    void rejectsASecondRedemptionEvenWithANewClientRequestId() {
        GoldBeanAccountEntity account = account(20_000L);
        GoldRobotRedemptionEntity existing = new GoldRobotRedemptionEntity();
        existing.setId(100L);
        existing.setEntitlementCode("ROBOT");
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(redemptionMapper.selectOne(any())).thenReturn(null, existing);
        authenticateCustomer();

        assertThatThrownBy(() -> service("https://cdn.example.test/robot-group.png")
                        .redeem(new RedeemGoldRobotRequest("robot-request-5")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能兑换一次");

        verify(goldBeanService, never())
                .debitDigitalBankForRobot(any(), any(BigDecimal.class), anyLong(), any(), any());
        verify(redemptionMapper, never()).insert(any(GoldRobotRedemptionEntity.class));
    }

    private GoldBeanRobotService service(String groupQrImageUrl) {
        return new GoldBeanRobotService(
                properties(groupQrImageUrl), goldBeanService, accountMapper, redemptionMapper);
    }

    private GoldBeanProperties properties(String groupQrImageUrl) {
        return new GoldBeanProperties(
                true,
                true,
                60,
                60,
                20,
                100,
                99_800,
                7,
                50,
                false,
                "",
                "",
                100,
                10_000,
                "机器人权益服务群",
                groupQrImageUrl);
    }

    private GoldBeanAccountEntity account(long digitalBankBalance) {
        GoldBeanAccountEntity account = new GoldBeanAccountEntity();
        account.setTenantId(2L);
        account.setUserId(3L);
        account.setRegistrationFeeStatus("PAID");
        account.setStatus("ACTIVE");
        account.setDigitalBankBalance(BigDecimal.valueOf(digitalBankBalance));
        account.setTradingBalance(BigDecimal.ZERO);
        account.setDeleted(0);
        return account;
    }

    private void authenticateCustomer() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                new CurrentPrincipal("jti", "customer", 3L, 2L, List.of(), List.of(), "CUSTOMER"), null));
    }
}
