package com.rayk.health.goldbean.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.dto.RedeemGoldRobotRequest;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldRobotRedemptionEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldRobotRedemptionMapper;
import com.rayk.health.goldbean.vo.GoldRobotRedemptionVo;
import com.rayk.health.goldbean.vo.GoldRobotStatusVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.rayk.health.goldbean.util.GoldBeanAmounts;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Redeems digital-bank gold beans for the robot-service entitlement. */
@Service
public class GoldBeanRobotService {
    public static final BigDecimal ROBOT_REDEMPTION_COST = BigDecimal.valueOf(10_000L);
    public static final String ROBOT_ENTITLEMENT_CODE = "ROBOT";
    public static final String ROBOT_ENTITLEMENT_NAME = "机器人权益";
    /** Marker resolved to the bundled mini-program QR image by the client. */
    public static final String BUNDLED_GROUP_QR_IMAGE_URL = "asset://robot-group-qr";

    private final GoldBeanProperties properties;
    private final GoldBeanApplicationService goldBeanService;
    private final GoldBeanAccountMapper accountMapper;
    private final GoldRobotRedemptionMapper redemptionMapper;

    public GoldBeanRobotService(
            GoldBeanProperties properties,
            GoldBeanApplicationService goldBeanService,
            GoldBeanAccountMapper accountMapper,
            GoldRobotRedemptionMapper redemptionMapper) {
        this.properties = properties;
        this.goldBeanService = goldBeanService;
        this.accountMapper = accountMapper;
        this.redemptionMapper = redemptionMapper;
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional(readOnly = true)
    public GoldRobotStatusVo status() {
        CurrentPrincipal current = requireEnabledCustomer();
        GoldBeanAccountEntity account = findAccount(current, false);
        boolean registered = account != null
                && "PAID".equals(account.getRegistrationFeeStatus())
                && "ACTIVE".equals(account.getStatus());
        BigDecimal digitalBankBalance = account == null
                ? GoldBeanAmounts.ZERO
                : GoldBeanAmounts.nonNegative(account.getDigitalBankBalance());
        long redeemedCount = account == null
                ? 0L
                : redemptionMapper.selectCount(new LambdaQueryWrapper<GoldRobotRedemptionEntity>()
                        .eq(GoldRobotRedemptionEntity::getTenantId, current.tenantId())
                        .eq(GoldRobotRedemptionEntity::getUserId, current.userId())
                        .eq(GoldRobotRedemptionEntity::getStatus, "COMPLETED")
                        .eq(GoldRobotRedemptionEntity::getDeleted, 0));
        boolean groupConfigured = StringUtils.hasText(resolveGroupQrImageUrl());
        // Keep the CTA available for every registered user who has not redeemed yet.
        // Balance and QR configuration are checked atomically by redeem(), so the
        // client can explain the exact unmet condition instead of hiding the action.
        return new GoldRobotStatusVo(
                properties.enabled(),
                registered,
                registered && redeemedCount == 0,
                groupConfigured,
                ROBOT_REDEMPTION_COST,
                digitalBankBalance,
                redeemedCount,
                ROBOT_ENTITLEMENT_NAME);
    }

    @PreAuthorize("hasAuthority('self:health-record') and principal.workbench == 'CUSTOMER'")
    @Transactional
    public GoldRobotRedemptionVo redeem(RedeemGoldRobotRequest request) {
        CurrentPrincipal current = requireEnabledCustomer();
        String clientRequestId = normalizeRequestId(request);
        GoldBeanAccountEntity account = findAccount(current, true);
        if (account == null) throw new BusinessException(ErrorCode.GOLD_BEAN_ACCOUNT_NOT_FOUND);

        GoldRobotRedemptionEntity existing = redemptionMapper.selectOne(
                new LambdaQueryWrapper<GoldRobotRedemptionEntity>()
                        .eq(GoldRobotRedemptionEntity::getTenantId, current.tenantId())
                        .eq(GoldRobotRedemptionEntity::getUserId, current.userId())
                        .eq(GoldRobotRedemptionEntity::getClientRequestId, clientRequestId)
                        .eq(GoldRobotRedemptionEntity::getDeleted, 0)
                        .last("LIMIT 1"));
        if (existing != null) return toVo(existing, account.getDigitalBankBalance());

        GoldRobotRedemptionEntity priorRedemption = redemptionMapper.selectOne(
                new LambdaQueryWrapper<GoldRobotRedemptionEntity>()
                        .eq(GoldRobotRedemptionEntity::getTenantId, current.tenantId())
                        .eq(GoldRobotRedemptionEntity::getUserId, current.userId())
                        .eq(GoldRobotRedemptionEntity::getEntitlementCode, ROBOT_ENTITLEMENT_CODE)
                        .last("LIMIT 1"));
        if (priorRedemption != null) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_ROBOT_ALREADY_REDEEMED);
        }

        goldBeanService.settleForPurchase(account, current.userId());
        if (!"PAID".equals(account.getRegistrationFeeStatus())
                || !"ACTIVE".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_REQUIRED);
        }
        BigDecimal balance = GoldBeanAmounts.nonNegative(account.getDigitalBankBalance());
        if (balance.compareTo(ROBOT_REDEMPTION_COST) < 0) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_ROBOT_BALANCE_INSUFFICIENT);
        }
        String groupQrImageUrl = resolveGroupQrImageUrl();

        LocalDateTime now = LocalDateTime.now();
        String redemptionNo = "GBRBT" + IdWorker.getId();
        goldBeanService.debitDigitalBankForRobot(
                account,
                ROBOT_REDEMPTION_COST,
                current.userId(),
                ledgerIdempotencyKey(current, clientRequestId),
                "兑换机器人权益");

        GoldRobotRedemptionEntity redemption = new GoldRobotRedemptionEntity();
        redemption.setId(IdWorker.getId());
        redemption.setTenantId(current.tenantId());
        redemption.setUserId(current.userId());
        redemption.setRedemptionNo(redemptionNo);
        redemption.setEntitlementCode(ROBOT_ENTITLEMENT_CODE);
        redemption.setGoldBeanCost(ROBOT_REDEMPTION_COST);
        redemption.setGroupName(properties.robotGroupName());
        redemption.setGroupQrImageUrl(groupQrImageUrl);
        redemption.setClientRequestId(clientRequestId);
        redemption.setStatus("COMPLETED");
        redemption.setRedeemedAt(now);
        redemption.setCreatedBy(current.userId());
        redemption.setCreatedAt(now);
        redemption.setUpdatedBy(current.userId());
        redemption.setUpdatedAt(now);
        redemption.setDeleted(0);
        redemption.setVersion(0);
        redemptionMapper.insert(redemption);
        return toVo(redemption, account.getDigitalBankBalance());
    }

    private String resolveGroupQrImageUrl() {
        String configured = properties.robotGroupQrImageUrl();
        return StringUtils.hasText(configured)
                ? configured.trim()
                : BUNDLED_GROUP_QR_IMAGE_URL;
    }

    private CurrentPrincipal requireEnabledCustomer() {
        if (!properties.enabled()) throw new BusinessException(ErrorCode.GOLD_BEAN_NOT_ENABLED);
        CurrentPrincipal current = CurrentUser.require();
        if (!"CUSTOMER".equals(current.workbench())) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        return current;
    }

    private GoldBeanAccountEntity findAccount(CurrentPrincipal current, boolean lock) {
        return accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                .eq(GoldBeanAccountEntity::getUserId, current.userId())
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last(lock ? "LIMIT 1 FOR UPDATE" : "LIMIT 1"));
    }

    private String normalizeRequestId(RedeemGoldRobotRequest request) {
        String requestId = request == null ? "" : request.clientRequestId();
        if (!StringUtils.hasText(requestId)) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        return requestId.trim();
    }

    private String ledgerIdempotencyKey(CurrentPrincipal current, String clientRequestId) {
        return "ROBOT_REDEEM:" + current.tenantId() + ":" + current.userId() + ":" + clientRequestId + ":BANK";
    }

    private GoldRobotRedemptionVo toVo(GoldRobotRedemptionEntity redemption, BigDecimal digitalBankBalance) {
        return new GoldRobotRedemptionVo(
                String.valueOf(redemption.getId()),
                redemption.getRedemptionNo(),
                redemption.getEntitlementCode(),
                ROBOT_ENTITLEMENT_NAME,
                redemption.getGoldBeanCost(),
                GoldBeanAmounts.nonNegative(digitalBankBalance),
                redemption.getGroupName(),
                redemption.getGroupQrImageUrl(),
                redemption.getRedeemedAt());
    }
}
