package com.rayk.health.goldbean.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanReferralAuthorizationEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanReferralAuthorizationMapper;
import com.rayk.health.goldbean.vo.GoldBeanReferralAuthorizationVo;
import com.rayk.health.membership.config.MembershipProperties;
import com.rayk.health.membership.payment.WeChatMerchantTransferClient;
import com.rayk.health.membership.payment.WechatMerchantTransferAuthorizationNotification;
import com.rayk.health.membership.payment.WeChatPayClient;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.wechat.entity.WeChatUserBindingEntity;
import com.rayk.health.security.wechat.mapper.WeChatUserBindingMapper;
import com.rayk.health.tenant.TenantContext;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Manages the one-time WeChat merchant-transfer receive authorization for referral rewards.
 *
 * <p>The authorization is deliberately separate from a referral payout. Existing and
 * unauthorised users continue to use the ordinary user-confirmation transfer flow. Only a
 * verified {@code TAKING_EFFECT} authorization can select the automatic transfer endpoint.
 */
@Service
public class GoldBeanReferralAuthorizationService {
    public static final String NOT_AUTHORIZED = "NOT_AUTHORIZED";
    public static final String REQUESTING = "REQUESTING";
    public static final String WAIT_USER_CONFIRM = "WAIT_USER_CONFIRM";
    public static final String TAKING_EFFECT = "TAKING_EFFECT";
    public static final String CLOSED = "CLOSED";

    private static final Logger log = LoggerFactory.getLogger(GoldBeanReferralAuthorizationService.class);

    private final GoldBeanProperties properties;
    private final GoldBeanApplicationService goldBeanService;
    private final GoldBeanAccountMapper accountMapper;
    private final GoldBeanReferralAuthorizationMapper authorizationMapper;
    private final WeChatUserBindingMapper bindingMapper;
    private final WeChatMerchantTransferClient merchantTransferClient;
    private final WeChatPayClient weChatPayClient;
    private final MembershipProperties membershipProperties;
    private final TransactionTemplate authorizationTransactionTemplate;
    private final boolean referralTransferEnabled;
    private final String referralTransferSceneId;
    private final String authorizationNotifyUrl;
    private final String referralTransferUserRecvPerception;
    private final int retrySeconds;

    public GoldBeanReferralAuthorizationService(
            GoldBeanProperties properties,
            GoldBeanApplicationService goldBeanService,
            GoldBeanAccountMapper accountMapper,
            GoldBeanReferralAuthorizationMapper authorizationMapper,
            WeChatUserBindingMapper bindingMapper,
            WeChatMerchantTransferClient merchantTransferClient,
            WeChatPayClient weChatPayClient,
            MembershipProperties membershipProperties,
            PlatformTransactionManager transactionManager,
            @Value("${rayk.gold-bean.registration-referral-transfer-enabled:false}")
                    boolean referralTransferEnabled,
            @Value("${rayk.gold-bean.registration-referral-transfer-scene-id:}")
                    String referralTransferSceneId,
            @Value("${rayk.gold-bean.registration-referral-authorization-notify-url:}")
                    String authorizationNotifyUrl,
            @Value("${rayk.gold-bean.registration-referral-transfer-user-recv-perception:劳务报酬}")
                    String referralTransferUserRecvPerception,
            @Value("${rayk.gold-bean.registration-referral-settlement-retry-seconds:30}") int retrySeconds) {
        this.properties = properties;
        this.goldBeanService = goldBeanService;
        this.accountMapper = accountMapper;
        this.authorizationMapper = authorizationMapper;
        this.bindingMapper = bindingMapper;
        this.merchantTransferClient = merchantTransferClient;
        this.weChatPayClient = weChatPayClient;
        this.membershipProperties = membershipProperties;
        this.authorizationTransactionTemplate = new TransactionTemplate(transactionManager);
        this.authorizationTransactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.referralTransferEnabled = referralTransferEnabled;
        this.referralTransferSceneId = normalize(referralTransferSceneId);
        this.authorizationNotifyUrl = normalize(authorizationNotifyUrl);
        this.referralTransferUserRecvPerception = normalizeOrDefault(referralTransferUserRecvPerception, "劳务报酬");
        this.retrySeconds = Math.max(10, Math.min(retrySeconds, 3600));
    }

    public GoldBeanReferralAuthorizationVo status() {
        CurrentPrincipal current = requireCustomer();
        if (!goldBeanService.available() || !properties.paymentEnabled()) {
            return empty(false);
        }
        boolean available = authorizationReady();
        GoldBeanReferralAuthorizationEntity authorization = findActive(
                current.tenantId(), current.userId(), membershipProperties.wechatPay().appId(), false);
        if (authorization == null) {
            authorization = findLatest(
                    current.tenantId(), current.userId(), membershipProperties.wechatPay().appId(), false);
        }
        return authorization == null ? empty(available) : toVo(authorization, available);
    }

    @Transactional
    public GoldBeanReferralAuthorizationVo beginAuthorization() {
        CurrentPrincipal current = requireCustomer();
        ensureAuthorizationReady();
        ensureRegistered(current);

        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        WeChatUserBindingEntity binding = activeBinding(current.tenantId(), current.userId(), pay.appId());
        if (binding == null || !StringUtils.hasText(binding.getOpenid())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRER_BINDING_REQUIRED);
        }

        // Lock the account so two quick taps cannot create two authorization applications.
        accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                .eq(GoldBeanAccountEntity::getUserId, current.userId())
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));

        GoldBeanReferralAuthorizationEntity authorization = findActive(
                current.tenantId(), current.userId(), pay.appId(), true);
        if (authorization != null) {
            return toVo(authorization, true);
        }
        authorization = findLatest(current.tenantId(), current.userId(), pay.appId(), true);
        if (authorization != null) {
            String state = normalizedState(authorization.getState());
            if (WAIT_USER_CONFIRM.equals(state) && StringUtils.hasText(authorization.getPackageInfo())) {
                return toVo(authorization, true);
            }
            if (REQUESTING.equals(state) || WAIT_USER_CONFIRM.equals(state)) {
                GoldBeanReferralAuthorizationVo synced = syncRecord(authorization, true);
                if (synced.authorized() || synced.userConfirmationRequired()) {
                    return synced;
                }
                // The query API may return WAIT_USER_CONFIRM without package_info. If an older
                // server already discarded the package, there is no package that the client can
                // reopen; create a fresh authorization request below. A transient
                // query failure keeps its retry reason and must not create duplicate requests.
                if (StringUtils.hasText(authorization.getFailureReason())
                        && !CLOSED.equals(normalizedState(authorization.getState()))) {
                    return synced;
                }
            }
        }

        authorization = newAuthorization(current, binding, pay.appId());
        authorizationMapper.insert(authorization);
        try {
            WeChatMerchantTransferClient.TransferAuthorization response =
                    merchantTransferClient.initiateAuthorization(
                            authorization.getOutAuthorizationNo(),
                            binding.getOpenid(),
                            referralTransferSceneId,
                            "三羊健康会员",
                            referralTransferUserRecvPerception,
                            authorizationNotifyUrl);
            applyResponse(authorization, response);
        } catch (RuntimeException exception) {
            markRetry(authorization, "微信自动收款授权申请暂时失败，请稍后重试");
            log.warn(
                    "Referral receive authorization requires reconciliation: userId={}, exceptionType={}",
                    current.userId(),
                    exception.getClass().getSimpleName());
        }
        return toVo(authorization, true);
    }

    @Transactional
    public GoldBeanReferralAuthorizationVo syncAuthorization() {
        CurrentPrincipal current = requireCustomer();
        if (!goldBeanService.available() || !properties.paymentEnabled()) {
            return empty(false);
        }
        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        GoldBeanReferralAuthorizationEntity authorization = findActive(
                current.tenantId(), current.userId(), pay.appId(), true);
        if (authorization == null) {
            authorization = findLatest(current.tenantId(), current.userId(), pay.appId(), true);
        }
        if (authorization == null) {
            return empty(authorizationReady());
        }
        if (!authorizationReady()) {
            return toVo(authorization, false);
        }
        if (TAKING_EFFECT.equals(normalizedState(authorization.getState()))) {
            return toVo(authorization, true);
        }
        return syncRecord(authorization, true);
    }

    /** Returns an active authorization id, or an empty string for the manual-confirmation path. */
    public String activeAuthorizationId(long tenantId, long userId, String openid) {
        if (!authorizationReady()) {
            return "";
        }
        return activeAuthorizationId(tenantId, userId, openid, referralTransferSceneId);
    }

    /**
     * Finds an active authorization for any configured transfer scene. Referral registration and
     * gold-bean market settlement share the same WeChat authorization table but may use different
     * scene ids.
     */
    public String activeAuthorizationId(
            long tenantId, long userId, String openid, String transferSceneId) {
        String normalizedSceneId = normalize(transferSceneId);
        if (!realWechatPaymentConfigured()
                || !StringUtils.hasText(openid)
                || !StringUtils.hasText(normalizedSceneId)) {
            return "";
        }
        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        GoldBeanReferralAuthorizationEntity authorization = authorizationMapper.selectOne(
                new LambdaQueryWrapper<GoldBeanReferralAuthorizationEntity>()
                        .eq(GoldBeanReferralAuthorizationEntity::getTenantId, tenantId)
                        .eq(GoldBeanReferralAuthorizationEntity::getUserId, userId)
                        .eq(GoldBeanReferralAuthorizationEntity::getAppId, pay.appId())
                        .eq(GoldBeanReferralAuthorizationEntity::getOpenid, openid)
                        .eq(GoldBeanReferralAuthorizationEntity::getTransferSceneId, normalizedSceneId)
                        .eq(GoldBeanReferralAuthorizationEntity::getState, TAKING_EFFECT)
                        .isNotNull(GoldBeanReferralAuthorizationEntity::getAuthorizationId)
                        .eq(GoldBeanReferralAuthorizationEntity::getDeleted, 0)
                        .orderByDesc(GoldBeanReferralAuthorizationEntity::getAuthorizedAt)
                        .orderByDesc(GoldBeanReferralAuthorizationEntity::getCreatedAt)
                        .last("LIMIT 1"));
        return authorization == null ? "" : normalize(authorization.getAuthorizationId());
    }

    /** Reconciles pending authorization applications without creating another authorization number. */
    public void recoverPendingAuthorizations() {
        if (!authorizationReady()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<GoldBeanReferralAuthorizationEntity> candidates = TenantContext.executeReadWithoutTenant(
                () -> authorizationMapper.selectList(new LambdaQueryWrapper<GoldBeanReferralAuthorizationEntity>()
                        .in(GoldBeanReferralAuthorizationEntity::getState, REQUESTING, WAIT_USER_CONFIRM)
                        .eq(GoldBeanReferralAuthorizationEntity::getDeleted, 0)
                        .and(query -> query.isNull(GoldBeanReferralAuthorizationEntity::getNextRetryAt)
                                .or()
                                .le(GoldBeanReferralAuthorizationEntity::getNextRetryAt, now))
                        .orderByAsc(GoldBeanReferralAuthorizationEntity::getCreatedAt)
                        .last("LIMIT 50")));
        for (GoldBeanReferralAuthorizationEntity candidate : candidates) {
            try {
                authorizationTransactionTemplate.executeWithoutResult(transactionStatus ->
                        TenantContext.execute(candidate.getTenantId(), () -> syncRecordByNumber(
                                candidate.getOutAuthorizationNo())));
            } catch (RuntimeException exception) {
                log.warn(
                        "Referral receive authorization recovery failed: recordId={}, exceptionType={}",
                        candidate.getId(),
                        exception.getClass().getSimpleName());
            }
        }
    }

    /** Handles the signature-verified, decrypted WeChat authorization result notification. */
    public void handleAuthorizationNotification(WechatMerchantTransferAuthorizationNotification result) {
        if (result == null
                || !StringUtils.hasText(result.out_authorization_no)
                || !StringUtils.hasText(result.appid)
                || !StringUtils.hasText(result.openid)
                || !StringUtils.hasText(result.state)) {
            throw new IllegalArgumentException("Incomplete merchant transfer authorization notification");
        }
        String outAuthorizationNo = normalize(result.out_authorization_no);
        List<GoldBeanReferralAuthorizationEntity> matches = TenantContext.executeReadWithoutTenant(
                () -> authorizationMapper.selectList(new LambdaQueryWrapper<GoldBeanReferralAuthorizationEntity>()
                        .eq(GoldBeanReferralAuthorizationEntity::getOutAuthorizationNo, outAuthorizationNo)
                        .eq(GoldBeanReferralAuthorizationEntity::getDeleted, 0)
                        .last("LIMIT 5")));
        GoldBeanReferralAuthorizationEntity candidate = matches.stream()
                .filter(item -> Objects.equals(item.getAppId(), result.appid)
                        && Objects.equals(item.getOpenid(), result.openid))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown merchant transfer authorization"));

        TenantContext.execute(candidate.getTenantId(), () -> {
            GoldBeanReferralAuthorizationEntity authorization = authorizationMapper.selectOne(
                    new LambdaQueryWrapper<GoldBeanReferralAuthorizationEntity>()
                            .eq(GoldBeanReferralAuthorizationEntity::getOutAuthorizationNo, outAuthorizationNo)
                            .eq(GoldBeanReferralAuthorizationEntity::getDeleted, 0)
                            .last("LIMIT 1 FOR UPDATE"));
            if (authorization == null
                    || !Objects.equals(authorization.getAppId(), result.appid)
                    || !Objects.equals(authorization.getOpenid(), result.openid)) {
                throw new IllegalStateException("Unknown merchant transfer authorization");
            }
            String state = normalizedState(result.state);
            if (TAKING_EFFECT.equals(state)) {
                if (!StringUtils.hasText(result.authorization_id)) {
                    throw new IllegalArgumentException("Missing merchant transfer authorization id");
                }
                authorization.setAuthorizationId(normalize(result.authorization_id));
                authorization.setState(TAKING_EFFECT);
                authorization.setPackageInfo(null);
                authorization.setAuthorizedAt(LocalDateTime.now());
                authorization.setFailureReason(null);
                authorization.setNextRetryAt(null);
            } else if (CLOSED.equals(state)) {
                authorization.setState(CLOSED);
                authorization.setPackageInfo(null);
                authorization.setFailureReason("微信自动收款授权已关闭，请重新申请");
                authorization.setNextRetryAt(null);
            } else {
                throw new IllegalArgumentException("Unsupported merchant transfer authorization state");
            }
            authorization.setLastCheckedAt(LocalDateTime.now());
            authorization.setUpdatedAt(LocalDateTime.now());
            authorization.setUpdatedBy(authorization.getUserId());
            authorizationMapper.updateById(authorization);
        });
    }

    private void syncRecordByNumber(String outAuthorizationNo) {
        GoldBeanReferralAuthorizationEntity authorization = authorizationMapper.selectOne(
                new LambdaQueryWrapper<GoldBeanReferralAuthorizationEntity>()
                        .eq(GoldBeanReferralAuthorizationEntity::getOutAuthorizationNo, outAuthorizationNo)
                        .eq(GoldBeanReferralAuthorizationEntity::getDeleted, 0)
                        .last("LIMIT 1 FOR UPDATE"));
        if (authorization != null
                && (REQUESTING.equals(normalizedState(authorization.getState()))
                        || WAIT_USER_CONFIRM.equals(normalizedState(authorization.getState())))) {
            syncRecord(authorization, false);
        }
    }

    private GoldBeanReferralAuthorizationVo syncRecord(
            GoldBeanReferralAuthorizationEntity authorization, boolean available) {
        try {
            WeChatMerchantTransferClient.TransferAuthorization response =
                    merchantTransferClient.queryAuthorization(authorization.getOutAuthorizationNo());
            applyResponse(authorization, response);
        } catch (WeChatMerchantTransferClient.TransferAuthorizationNotFoundException exception) {
            authorization.setState(CLOSED);
            authorization.setPackageInfo(null);
            authorization.setFailureReason("授权申请不存在，请重新申请");
            authorization.setLastCheckedAt(LocalDateTime.now());
            authorization.setNextRetryAt(null);
            authorization.setUpdatedAt(LocalDateTime.now());
            authorization.setUpdatedBy(authorization.getUserId());
            authorizationMapper.updateById(authorization);
        } catch (RuntimeException exception) {
            markRetry(authorization, "微信自动收款授权状态暂时无法确认，请稍后刷新");
            log.warn(
                    "Referral receive authorization query requires retry: recordId={}, exceptionType={}",
                    authorization.getId(),
                    exception.getClass().getSimpleName());
        }
        return toVo(authorization, available);
    }

    void applyResponse(
            GoldBeanReferralAuthorizationEntity authorization,
            WeChatMerchantTransferClient.TransferAuthorization response) {
        LocalDateTime now = LocalDateTime.now();
        String state = normalizedState(response == null ? null : response.state());
        String responseAuthorizationId = normalize(response == null ? null : response.authorizationId());
        String responsePackageInfo = normalize(response == null ? null : response.packageInfo());
        String existingAuthorizationId = normalize(authorization.getAuthorizationId());
        String existingPackageInfo = normalize(authorization.getPackageInfo());
        authorization.setAuthorizationCreatedAt(
                authorization.getAuthorizationCreatedAt() == null ? now : authorization.getAuthorizationCreatedAt());

        if (TAKING_EFFECT.equals(state)) {
            // Some status responses omit authorization_id even though the local record already
            // has the id from the signed callback. Do not turn an active authorization back into
            // a pending one just because a response field is absent.
            String effectiveAuthorizationId = StringUtils.hasText(responseAuthorizationId)
                    ? responseAuthorizationId
                    : existingAuthorizationId;
            authorization.setAuthorizationId(
                    StringUtils.hasText(effectiveAuthorizationId) ? effectiveAuthorizationId : null);
            authorization.setPackageInfo(null);
            if (!StringUtils.hasText(effectiveAuthorizationId)) {
                state = REQUESTING;
                authorization.setFailureReason("微信授权结果尚未完整返回，系统将继续查询");
            } else {
                authorization.setFailureReason(null);
            }
        } else if (CLOSED.equals(state)) {
            authorization.setAuthorizationId(null);
            authorization.setPackageInfo(null);
            authorization.setFailureReason("微信自动收款授权已关闭，请重新申请");
        } else {
            authorization.setAuthorizationId(null);
            // The authorization query normally returns only the state. Retain the package from
            // the create response until the user completes the authorization page.
            String effectivePackageInfo = StringUtils.hasText(responsePackageInfo)
                    ? responsePackageInfo
                    : existingPackageInfo;
            authorization.setPackageInfo(StringUtils.hasText(effectivePackageInfo) ? effectivePackageInfo : null);
            authorization.setFailureReason(null);
        }
        authorization.setState(state);
        authorization.setLastCheckedAt(now);
        if (TAKING_EFFECT.equals(state)) {
            authorization.setAuthorizedAt(authorization.getAuthorizedAt() == null ? now : authorization.getAuthorizedAt());
            authorization.setNextRetryAt(null);
            authorization.setPackageInfo(null);
        } else if (CLOSED.equals(state)) {
            authorization.setNextRetryAt(null);
        } else {
            authorization.setNextRetryAt(now.plusSeconds(retrySeconds));
        }
        authorization.setUpdatedAt(now);
        authorization.setUpdatedBy(authorization.getUserId());
        authorizationMapper.updateById(authorization);
    }

    private void markRetry(GoldBeanReferralAuthorizationEntity authorization, String reason) {
        LocalDateTime now = LocalDateTime.now();
        String state = normalizedState(authorization.getState());
        authorization.setState(
                WAIT_USER_CONFIRM.equals(state) ? WAIT_USER_CONFIRM : REQUESTING);
        authorization.setFailureReason(reason);
        authorization.setLastCheckedAt(now);
        authorization.setNextRetryAt(now.plusSeconds(retrySeconds));
        authorization.setUpdatedAt(now);
        authorization.setUpdatedBy(authorization.getUserId());
        authorizationMapper.updateById(authorization);
    }

    private GoldBeanReferralAuthorizationEntity newAuthorization(
            CurrentPrincipal current, WeChatUserBindingEntity binding, String appId) {
        LocalDateTime now = LocalDateTime.now();
        GoldBeanReferralAuthorizationEntity authorization = new GoldBeanReferralAuthorizationEntity();
        authorization.setId(IdWorker.getId());
        authorization.setTenantId(current.tenantId());
        authorization.setUserId(current.userId());
        authorization.setAppId(appId);
        authorization.setOpenid(binding.getOpenid());
        authorization.setTransferSceneId(referralTransferSceneId);
        authorization.setOutAuthorizationNo("GRA" + Long.toUnsignedString(IdWorker.getId()));
        authorization.setState(REQUESTING);
        authorization.setCreatedBy(current.userId());
        authorization.setCreatedAt(now);
        authorization.setUpdatedBy(current.userId());
        authorization.setUpdatedAt(now);
        authorization.setDeleted(0);
        authorization.setVersion(0);
        return authorization;
    }

    private void ensureRegistered(CurrentPrincipal current) {
        GoldBeanAccountEntity account = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                .eq(GoldBeanAccountEntity::getUserId, current.userId())
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (account == null) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_ACCOUNT_NOT_FOUND);
        }
        if (!"ACTIVE".equals(account.getStatus()) || !"PAID".equals(account.getRegistrationFeeStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_REQUIRED);
        }
    }

    private GoldBeanReferralAuthorizationEntity findActive(
            long tenantId, long userId, String appId, boolean lock) {
        LambdaQueryWrapper<GoldBeanReferralAuthorizationEntity> query = new LambdaQueryWrapper<GoldBeanReferralAuthorizationEntity>()
                .eq(GoldBeanReferralAuthorizationEntity::getTenantId, tenantId)
                .eq(GoldBeanReferralAuthorizationEntity::getUserId, userId)
                .eq(GoldBeanReferralAuthorizationEntity::getAppId, appId)
                .eq(GoldBeanReferralAuthorizationEntity::getTransferSceneId, referralTransferSceneId)
                .eq(GoldBeanReferralAuthorizationEntity::getState, TAKING_EFFECT)
                .isNotNull(GoldBeanReferralAuthorizationEntity::getAuthorizationId)
                .eq(GoldBeanReferralAuthorizationEntity::getDeleted, 0)
                .orderByDesc(GoldBeanReferralAuthorizationEntity::getAuthorizedAt)
                .orderByDesc(GoldBeanReferralAuthorizationEntity::getCreatedAt);
        query.last(lock ? "LIMIT 1 FOR UPDATE" : "LIMIT 1");
        return authorizationMapper.selectOne(query);
    }

    private GoldBeanReferralAuthorizationEntity findLatest(
            long tenantId, long userId, String appId, boolean lock) {
        LambdaQueryWrapper<GoldBeanReferralAuthorizationEntity> query = new LambdaQueryWrapper<GoldBeanReferralAuthorizationEntity>()
                .eq(GoldBeanReferralAuthorizationEntity::getTenantId, tenantId)
                .eq(GoldBeanReferralAuthorizationEntity::getUserId, userId)
                .eq(GoldBeanReferralAuthorizationEntity::getAppId, appId)
                .eq(GoldBeanReferralAuthorizationEntity::getTransferSceneId, referralTransferSceneId)
                .eq(GoldBeanReferralAuthorizationEntity::getDeleted, 0)
                .orderByDesc(GoldBeanReferralAuthorizationEntity::getCreatedAt);
        query.last(lock ? "LIMIT 1 FOR UPDATE" : "LIMIT 1");
        return authorizationMapper.selectOne(query);
    }

    private WeChatUserBindingEntity activeBinding(long tenantId, long userId, String appId) {
        return bindingMapper.selectOne(new LambdaQueryWrapper<WeChatUserBindingEntity>()
                .eq(WeChatUserBindingEntity::getTenantId, tenantId)
                .eq(WeChatUserBindingEntity::getUserId, userId)
                .eq(WeChatUserBindingEntity::getAppId, appId)
                .eq(WeChatUserBindingEntity::getStatus, "ACTIVE")
                .eq(WeChatUserBindingEntity::getDeleted, 0)
                .last("LIMIT 1"));
    }

    private boolean authorizationReady() {
        return referralTransferEnabled
                && StringUtils.hasText(referralTransferSceneId)
                && StringUtils.hasText(authorizationNotifyUrl)
                && validHttpsUrl(authorizationNotifyUrl)
                && realWechatPaymentConfigured();
    }

    private boolean realWechatPaymentConfigured() {
        return goldBeanService.available()
                && properties.paymentEnabled()
                && membershipProperties.wechatPay().configured()
                && weChatPayClient.configured();
    }

    private void ensureAuthorizationReady() {
        if (!authorizationReady()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_PAYMENT_NOT_CONFIGURED);
        }
    }

    private boolean validHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && StringUtils.hasText(uri.getHost())
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private GoldBeanReferralAuthorizationVo toVo(
            GoldBeanReferralAuthorizationEntity authorization, boolean available) {
        String state = normalizedState(authorization.getState());
        boolean authorized = TAKING_EFFECT.equals(state) && StringUtils.hasText(authorization.getAuthorizationId());
        boolean confirmation = WAIT_USER_CONFIRM.equals(state)
                && StringUtils.hasText(authorization.getPackageInfo());
        return new GoldBeanReferralAuthorizationVo(
                state,
                available,
                authorized,
                confirmation,
                confirmation ? membershipProperties.wechatPay().merchantId() : null,
                confirmation ? membershipProperties.wechatPay().appId() : null,
                confirmation ? authorization.getPackageInfo() : null,
                authorized ? authorization.getAuthorizedAt() : null,
                authorization.getLastCheckedAt(),
                safeFailure(authorization.getFailureReason()));
    }

    private GoldBeanReferralAuthorizationVo empty(boolean available) {
        return new GoldBeanReferralAuthorizationVo(
                NOT_AUTHORIZED, available, false, false, null, null, null, null, null, null);
    }

    private CurrentPrincipal requireCustomer() {
        CurrentPrincipal current = CurrentUser.require();
        if (!"CUSTOMER".equals(current.workbench())) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        return current;
    }

    private String normalizedState(String value) {
        String normalized = normalize(value).toUpperCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? REQUESTING : normalized;
    }

    private String safeFailure(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    private String normalizeOrDefault(String value, String fallback) {
        String normalized = normalize(value);
        return normalized.isBlank() ? fallback : normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
