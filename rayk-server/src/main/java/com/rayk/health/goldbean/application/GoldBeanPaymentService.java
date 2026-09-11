package com.rayk.health.goldbean.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayk.health.common.util.TextEncodingUtils;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.dto.CreateGoldBeanPurchaseOrderRequest;
import com.rayk.health.goldbean.dto.CreateGoldBeanRegistrationOrderRequest;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanOrderEntity;
import com.rayk.health.goldbean.entity.GoldBeanPlatformInviteEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanOrderMapper;
import com.rayk.health.goldbean.util.GoldBeanAmounts;
import com.rayk.health.goldbean.vo.GoldBeanReferralSettlementVo;
import com.rayk.health.goldbean.vo.GoldBeanOrderVo;
import com.rayk.health.membership.config.MembershipProperties;
import com.rayk.health.membership.payment.WeChatMerchantTransferClient;
import com.rayk.health.membership.payment.WeChatPayClient;
import com.rayk.health.membership.payment.WeChatVirtualPayClient;
import com.rayk.health.membership.vo.MembershipPaymentVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.wechat.entity.WeChatUserBindingEntity;
import com.rayk.health.security.wechat.mapper.WeChatUserBindingMapper;
import com.rayk.health.tenant.TenantContext;
import com.wechat.pay.java.service.payments.model.Transaction;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/** Real-payment order and delivery handling for platform-collected gold beans. */
@Service
public class GoldBeanPaymentService {
    private static final Logger log = LoggerFactory.getLogger(GoldBeanPaymentService.class);
    private static final String REGISTRATION_ORDER_PREFIX = "GBR";
    private static final String PURCHASE_ORDER_PREFIX = "GBP";
    private static final String REGISTRATION_TYPE = "REGISTRATION_FEE";
    private static final String PURCHASE_TYPE = "GOLD_BEAN_PURCHASE";
    private static final String LEGENDARY_BANK_PURCHASE_TYPE = "LEGENDARY_BANK_PURCHASE";
    private static final String PENDING = "PENDING";
    private static final String PAID = "PAID";
    private static final String CLOSED = "CLOSED";
    private static final String PLATFORM = "PLATFORM";
    private static final String REFERRER = "REFERRER";
    private static final String SETTLEMENT_NOT_REQUIRED = "NOT_REQUIRED";
    private static final String SETTLEMENT_PENDING = "PENDING";
    private static final String SETTLEMENT_SETTLED = "SETTLED";
    private static final String SETTLEMENT_FAILED = "FAILED";

    private final GoldBeanProperties properties;
    private final GoldBeanApplicationService goldBeanService;
    private final GoldBeanAccountMapper accountMapper;
    private final GoldBeanOrderMapper orderMapper;
    private final WeChatUserBindingMapper bindingMapper;
    private final WeChatPayClient weChatPayClient;
    private final WeChatMerchantTransferClient merchantTransferClient;
    private final GoldBeanReferralAuthorizationService referralAuthorizationService;
    private final MembershipProperties membershipProperties;
    private final WeChatVirtualPayClient virtualPayClient;
    private final ObjectMapper objectMapper;
    private final GoldBeanPlatformInviteService platformInviteService;
    private final GoldBeanLegendaryService legendaryService;
    private final TransactionTemplate settlementTransactionTemplate;
    private final boolean referralTransferEnabled;
    private final String referralTransferSceneId;
    private final String referralTransferNotifyUrl;
    private final String referralTransferUserRecvPerception;
    private final String referralTransferJobType;
    private final String referralTransferRewardDescription;
    private final int referralSettlementRetrySeconds;

    public GoldBeanPaymentService(
            GoldBeanProperties properties,
            GoldBeanApplicationService goldBeanService,
            GoldBeanAccountMapper accountMapper,
            GoldBeanOrderMapper orderMapper,
            WeChatUserBindingMapper bindingMapper,
            WeChatPayClient weChatPayClient,
            WeChatMerchantTransferClient merchantTransferClient,
            GoldBeanReferralAuthorizationService referralAuthorizationService,
            MembershipProperties membershipProperties,
            WeChatVirtualPayClient virtualPayClient,
            ObjectMapper objectMapper,
            GoldBeanPlatformInviteService platformInviteService,
            GoldBeanLegendaryService legendaryService,
            PlatformTransactionManager transactionManager,
            @Value("${rayk.gold-bean.registration-referral-transfer-enabled:false}") boolean referralTransferEnabled,
            @Value("${rayk.gold-bean.registration-referral-transfer-scene-id:}") String referralTransferSceneId,
            @Value("${rayk.gold-bean.registration-referral-transfer-notify-url:}") String referralTransferNotifyUrl,
            @Value("${rayk.gold-bean.registration-referral-transfer-user-recv-perception:劳务报酬}")
                    String referralTransferUserRecvPerception,
            @Value("${rayk.gold-bean.registration-referral-transfer-job-type:推荐注册服务}")
                    String referralTransferJobType,
            @Value("${rayk.gold-bean.registration-referral-transfer-reward-description:推荐注册服务报酬}")
                    String referralTransferRewardDescription,
            @Value("${rayk.gold-bean.registration-referral-settlement-retry-seconds:30}")
                    int referralSettlementRetrySeconds) {
        this.properties = properties;
        this.goldBeanService = goldBeanService;
        this.accountMapper = accountMapper;
        this.orderMapper = orderMapper;
        this.bindingMapper = bindingMapper;
        this.weChatPayClient = weChatPayClient;
        this.merchantTransferClient = merchantTransferClient;
        this.referralAuthorizationService = referralAuthorizationService;
        this.membershipProperties = membershipProperties;
        this.virtualPayClient = virtualPayClient;
        this.objectMapper = objectMapper;
        this.platformInviteService = platformInviteService;
        this.legendaryService = legendaryService;
        this.settlementTransactionTemplate = new TransactionTemplate(transactionManager);
        this.settlementTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.referralTransferEnabled = referralTransferEnabled;
        this.referralTransferSceneId = referralTransferSceneId == null ? "" : referralTransferSceneId.trim();
        this.referralTransferNotifyUrl = referralTransferNotifyUrl == null ? "" : referralTransferNotifyUrl.trim();
        this.referralTransferUserRecvPerception = normalizeTransferText(referralTransferUserRecvPerception, "劳务报酬");
        this.referralTransferJobType = normalizeTransferText(referralTransferJobType, "推荐注册服务");
        this.referralTransferRewardDescription = normalizeTransferText(
                referralTransferRewardDescription, "推荐注册服务报酬");
        this.referralSettlementRetrySeconds = Math.max(10, Math.min(referralSettlementRetrySeconds, 3600));
    }

    @Transactional
    public GoldBeanOrderVo createRegistrationOrder(CreateGoldBeanRegistrationOrderRequest request) {
        CurrentPrincipal current = requireCustomer();
        closeExpiredPlatformRegistrationOrders();
        GoldBeanAccountEntity account = goldBeanService.ensureAccount(current);
        if (PAID.equals(account.getRegistrationFeeStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_ALREADY_REGISTERED);
        }

        String referralCode = normalize(request == null ? null : request.referralCode());
        String platformInviteCode = normalize(request == null ? null : request.platformInviteCode());
        if (!referralCode.isBlank() && !platformInviteCode.isBlank()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REGISTRATION_CODE_CONFLICT);
        }
        String city = normalizeCity(request == null ? null : request.city());
        if (StringUtils.hasText(account.getCity()) && !city.isBlank() && !city.equals(account.getCity())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_INVALID);
        }

        GoldBeanPlatformInviteEntity platformInvite = null;
        GoldBeanAccountEntity referrer = null;
        String orderNo = "GBR" + IdWorker.getId();
        if (referralCode.isBlank()) {
            if (goldBeanService.hasPlatformRegistration()) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_REGISTRATION_ONLY);
            }
            requirePaymentProduct(properties.registrationProductId());
            platformInvite = platformInviteService.reserve(platformInviteCode, current, orderNo);
            orderNo = platformInvite.getReservedOrderNo();
        } else {
            referrer = findActiveReferrer(current.tenantId(), account.getUserId(), referralCode);
            requirePaymentProduct(properties.referralRegistrationProductId());
            ensureReferralPaymentConfigured(current.tenantId(), referrer.getUserId());
        }

        LocalDateTime now = LocalDateTime.now();
        GoldBeanOrderEntity order = newOrder(current, REGISTRATION_ORDER_PREFIX, REGISTRATION_TYPE, now, orderNo);
        int businessAmountCent = registrationFeeCent(referrer == null ? PLATFORM : REFERRER);
        order.setAmountCent(businessAmountCent);
        order.setPaymentAmountCent(properties.surchargedPaymentAmountCent(businessAmountCent));
        order.setGoldBeanQuantity(GoldBeanAmounts.ZERO);
        order.setRegistrationReferralCode(referralCode.isBlank() ? null : referralCode);
        order.setRegistrationReferrerId(referrer == null ? null : referrer.getUserId());
        order.setRegistrationCity(city.isBlank() ? null : city);
        order.setRegistrationFeeRecipient(referrer == null ? PLATFORM : REFERRER);
        order.setSettlementStatus(referrer == null ? SETTLEMENT_NOT_REQUIRED : SETTLEMENT_PENDING);
        if (platformInvite != null) {
            order.setPlatformInviteId(platformInvite.getId());
            order.setPlatformSlotKey(GoldBeanPlatformInviteService.PLATFORM_ROOT_SLOT);
        }
        order.setProductId(referrer == null
                ? properties.registrationProductId()
                : properties.referralRegistrationProductId());
        order.setExpiresAt(now.plusMinutes(30));
        try {
            orderMapper.insert(order);
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            if (platformInvite != null) {
                platformInviteService.release(platformInvite.getId(), orderNo, current.userId());
                throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_REGISTRATION_ONLY);
            }
            throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
        }
        return toVo(order);
    }

    @Transactional
    public GoldBeanOrderVo createPurchaseOrder(CreateGoldBeanPurchaseOrderRequest request) {
        CurrentPrincipal current = requireCustomer();
        // Legendary users have a separate market-only path. Keep this check on the server so an
        // old or hand-crafted client cannot bypass the legendary market restriction.
        if (legendaryService.isEligible(current.userId())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_PURCHASE_DISABLED);
        }
        GoldBeanAccountEntity account = goldBeanService.ensureAccount(current);
        goldBeanService.settleForPurchase(account, current.userId());
        goldBeanService.requirePlatformPurchaseEligible(account);

        BigDecimal quantity = GoldBeanAmounts.normalize(request == null ? null : request.quantity());
        if (!GoldBeanAmounts.positive(quantity)
                || quantity.compareTo(BigDecimal.valueOf(properties.maxPurchaseQuantity())) > 0) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        int amountCent;
        try {
            amountCent = Math.toIntExact(
                    GoldBeanAmounts.currencyCents(quantity, properties.goldBeanUnitPriceCent()));
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        requirePaymentProduct(properties.goldBeanProductId());

        LocalDateTime now = LocalDateTime.now();
        GoldBeanOrderEntity order = newOrder(current, PURCHASE_ORDER_PREFIX, PURCHASE_TYPE, now, null);
        order.setAmountCent(amountCent);
        order.setPaymentAmountCent(properties.surchargedPaymentAmountCent(amountCent));
        order.setGoldBeanQuantity(quantity);
        order.setProductId(properties.goldBeanProductId());
        order.setSettlementStatus(SETTLEMENT_NOT_REQUIRED);
        order.setExpiresAt(now.plusMinutes(30));
        orderMapper.insert(order);
        return toVo(order);
    }

    /**
     * The legacy direct-platform purchase endpoint is intentionally disabled.
     * Existing orders of this type remain deliverable through the callback path.
     */
    @Transactional
    public GoldBeanOrderVo createLegendaryBankPurchaseOrder(CreateGoldBeanPurchaseOrderRequest request) {
        requireCustomer();
        throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_PURCHASE_DISABLED);
    }

    @Transactional
    public GoldBeanOrderVo order(String orderNo) {
        GoldBeanOrderEntity order = ownedOrderForUpdate(orderNo);
        closeIfExpired(order);
        return toVo(order);
    }

    /** Creates the virtual-payment parameters for either platform or referral registration. */
    @Transactional
    public MembershipPaymentVo createWechatPayment(String orderNo) {
        CurrentPrincipal current = requireCustomer();
        GoldBeanOrderEntity order = ownedOrderForUpdate(orderNo);
        closeIfExpired(order);
        if (!PENDING.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_ORDER_INVALID_STATUS);
        }
        if (LEGENDARY_BANK_PURCHASE_TYPE.equals(order.getOrderType())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_PURCHASE_DISABLED);
        }
        requirePaymentProduct(order.getProductId());
        int quantity = paymentGoodsQuantity(order);
        int goodsPrice = paymentGoodsPrice(order);
        return virtualPayClient.createGoodsPayment(
                order.getOrderNo(), quantity, order.getProductId(), goodsPrice, current.userId(),
                "gold-bean:" + order.getOrderNo());
    }

    @Transactional
    public GoldBeanOrderVo cancelOrder(String orderNo) {
        GoldBeanOrderEntity order = ownedOrderForUpdate(orderNo);
        if (PAID.equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_ORDER_INVALID_STATUS);
        }
        if (!PENDING.equals(order.getStatus())) {
            return toVo(order);
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(CLOSED);
        order.setPlatformSlotKey(null);
        if (REFERRER.equals(order.getRegistrationFeeRecipient())) {
            order.setSettlementStatus(SETTLEMENT_NOT_REQUIRED);
        }
        order.setUpdatedBy(order.getCustomerId());
        order.setUpdatedAt(now);
        orderMapper.updateById(order);
        platformInviteService.release(order.getPlatformInviteId(), order.getOrderNo(), order.getCustomerId());
        return toVo(order);
    }

    /** Handles the verified-by-content WeChat virtual-payment delivery push. */
    @Transactional
    public void handleVirtualPaymentNotification(String body) {
        handleVirtualPaymentNotification(body, false);
    }

    /**
     * Handles both virtual-payment callback envelopes supported by the platform:
     * the newer payload/payEventSig envelope and the standard mini-program message-push
     * JSON body, which is authenticated by the signed message-push query parameters.
     */
    @Transactional
    public void handleVirtualPaymentNotification(String body, boolean messagePushAuthenticated) {
        String orderNo = "";
        String stage = "parse";
        try {
            JsonNode root = objectMapper.readTree(body);
            stage = "event";
            String event = text(root, "event", "Event");
            String eventType = text(root, "eventType", "EventType", "event_type", "eventtype");
            if (!isSupportedVirtualPaymentEvent(event, eventType)) {
                log.warn(
                        "Virtual payment callback rejected at event: event={}, eventType={}",
                        event,
                        eventType);
                throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
            }
            String payloadText = payloadText(root);
            boolean directMessagePush = !StringUtils.hasText(payloadText);
            stage = "event_signature";
            if (directMessagePush) {
                if (!messagePushAuthenticated || !hasDirectGoodsDeliveryFields(root)) {
                    log.warn(
                            "Virtual payment callback rejected at message push authentication: orderNo={}, authenticated={}, hasDirectFields={}",
                            orderNo,
                            messagePushAuthenticated,
                            hasDirectGoodsDeliveryFields(root));
                    throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
                }
            } else if (!virtualPayClient.verifyPaymentEventSignature(
                    event, payloadText, text(root, "payEventSig", "PayEventSig"))) {
                log.warn(
                        "Virtual payment callback rejected at event signature: orderNo={}, hasPayload={}, hasPayEventSig={}",
                        orderNo,
                        StringUtils.hasText(payloadText),
                        StringUtils.hasText(text(root, "payEventSig", "PayEventSig")));
                throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
            }
            JsonNode payload = directMessagePush ? root : objectMapper.readTree(payloadText);
            String payloadOrderNo = text(payload, "OutTradeNo", "outTradeNo", "out_trade_no");
            orderNo = text(root, "outTradeNo", "OutTradeNo", "out_trade_no");
            if (!StringUtils.hasText(orderNo)) {
                // Legacy goods-delivery callbacks may carry the merchant order only in Payload.
                orderNo = payloadOrderNo;
            }
            if (!StringUtils.hasText(orderNo) || !isGoldBeanOrder(orderNo)) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_ORDER_NOT_FOUND);
            }
            if (StringUtils.hasText(payloadOrderNo) && !orderNo.equals(payloadOrderNo)) {
                log.warn(
                        "Virtual payment callback rejected at payload order match: orderNo={}, payloadOrderPresent={}",
                        orderNo,
                        StringUtils.hasText(payloadOrderNo));
                throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
            }
            stage = "order_lookup";
            GoldBeanOrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<GoldBeanOrderEntity>()
                    .eq(GoldBeanOrderEntity::getOrderNo, orderNo)
                    .eq(GoldBeanOrderEntity::getDeleted, 0)
                    .last("LIMIT 1 FOR UPDATE"));
            if (order == null) throw new BusinessException(ErrorCode.GOLD_BEAN_ORDER_NOT_FOUND);
            if (PAID.equals(order.getStatus())) return;
            if (!PENDING.equals(order.getStatus())) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_ORDER_INVALID_STATUS);
            }

            MembershipProperties.WeChatVirtualPayProperties pay = virtualPayClient.paymentProperties();
            int expectedQuantity = paymentGoodsQuantity(order);
            int expectedPrice = paymentGoodsPrice(order);
            int env = integer(payload, "Env", "env");
            if (env < 0) env = integer(root, "Env", "env");
            String openid = text(payload, "OpenId", "openid");
            JsonNode goods = child(payload, "GoodsInfo", "goodsInfo");
            JsonNode payment = child(payload, "PayInfo", "payInfo", "WeChatPayInfo", "weChatPayInfo");
            String productId = text(goods, "ProductId", "productId");
            int quantity = integer(goods, "Quantity", "quantity");
            int actualPrice = integer(goods, "ActualPrice", "actualPrice");
            String payloadTransactionId = text(payment, "TransactionId", "transactionId");
            String outerTransactionId = text(root, "transactionId", "TransactionId");
            String transactionId = StringUtils.hasText(payloadTransactionId)
                    ? payloadTransactionId
                    : outerTransactionId;
            String merchantCode = text(root, "MerchantCode", "merchantCode");
            if (!StringUtils.hasText(merchantCode)) {
                merchantCode = text(payload, "MerchantCode", "merchantCode");
            }
            boolean productConfigured = virtualPayClient.configuredFor(order.getProductId());
            boolean envMatches = env < 0 || env == pay.env();
            boolean productMatches = order.getProductId().equals(productId);
            boolean quantityMatches = quantity == expectedQuantity;
            boolean priceMatches = actualPrice == expectedPrice;
            boolean openidPresent = StringUtils.hasText(openid);
            boolean transactionPresent = StringUtils.hasText(transactionId);
            boolean outerTransactionMatches = !StringUtils.hasText(outerTransactionId)
                    || !StringUtils.hasText(payloadTransactionId)
                    || outerTransactionId.equals(payloadTransactionId);
            boolean merchantCodeMatches = !StringUtils.hasText(merchantCode)
                    || pay.merchantId().equals(merchantCode);

            if (!productConfigured || !envMatches || !productMatches || !quantityMatches || !priceMatches
                    || !openidPresent || !transactionPresent || !outerTransactionMatches || !merchantCodeMatches) {
                log.warn(
                        "Virtual payment callback rejected at payment fields: orderNo={}, productConfigured={}, env={}, expectedEnv={}, envMatches={}, productMatches={}, quantity={}, expectedQuantity={}, price={}, expectedPrice={}, openidPresent={}, transactionPresent={}, outerTransactionMatches={}, merchantCodeMatches={}",
                        orderNo,
                        productConfigured,
                        env,
                        pay.env(),
                        envMatches,
                        productMatches,
                        quantity,
                        expectedQuantity,
                        actualPrice,
                        expectedPrice,
                        openidPresent,
                        transactionPresent,
                        outerTransactionMatches,
                        merchantCodeMatches);
                throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
            }
            stage = "payer_binding";
            WeChatUserBindingEntity binding = bindingMapper.selectOne(
                    new LambdaQueryWrapper<WeChatUserBindingEntity>()
                            .eq(WeChatUserBindingEntity::getTenantId, order.getTenantId())
                            .eq(WeChatUserBindingEntity::getUserId, order.getCustomerId())
                            .eq(WeChatUserBindingEntity::getAppId, pay.appId())
                            .eq(WeChatUserBindingEntity::getOpenid, openid)
                            .eq(WeChatUserBindingEntity::getStatus, "ACTIVE")
                            .eq(WeChatUserBindingEntity::getDeleted, 0)
                            .last("LIMIT 1"));
            if (binding == null) {
                log.warn("Virtual payment callback rejected at payer binding: orderNo={}", orderNo);
                throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
            }
            stage = "duplicate_transaction";
            if (transactionPresent) {
                GoldBeanOrderEntity duplicateTransaction = orderMapper.selectOne(
                        new LambdaQueryWrapper<GoldBeanOrderEntity>()
                                .eq(GoldBeanOrderEntity::getTransactionId, transactionId)
                                .ne(GoldBeanOrderEntity::getOrderNo, orderNo)
                                .eq(GoldBeanOrderEntity::getDeleted, 0)
                                .last("LIMIT 1"));
                if (duplicateTransaction != null) {
                    log.warn("Virtual payment callback rejected at duplicate transaction: orderNo={}", orderNo);
                    throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
                }
            }

            if (REGISTRATION_TYPE.equals(order.getOrderType())) {
                stage = "registration";
                if (PLATFORM.equals(order.getRegistrationFeeRecipient())) {
                    if (order.getPlatformInviteId() == null) {
                        throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_INVITE_REQUIRED);
                    }
                    platformInviteService.consume(order.getPlatformInviteId(), orderNo, order.getCustomerId());
                    goldBeanService.completeRegistration(
                            order.getTenantId(), order.getCustomerId(), order.getRegistrationReferralCode(),
                            null, order.getRegistrationCity(), PLATFORM, orderNo);
                } else if (REFERRER.equals(order.getRegistrationFeeRecipient())) {
                    if (order.getRegistrationReferrerId() == null
                            || !StringUtils.hasText(order.getRegistrationReferralCode())) {
                        throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_PAYMENT_NOT_CONFIGURED);
                    }
                    goldBeanService.completeRegistration(
                            order.getTenantId(), order.getCustomerId(), order.getRegistrationReferralCode(),
                            order.getRegistrationReferrerId(), order.getRegistrationCity(), REFERRER, orderNo);
                } else {
                    throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
                }
            } else if (PURCHASE_TYPE.equals(order.getOrderType())) {
                goldBeanService.completePlatformPurchase(
                        order.getTenantId(), order.getCustomerId(), order.getGoldBeanQuantity(), orderNo);
            } else if (LEGENDARY_BANK_PURCHASE_TYPE.equals(order.getOrderType())) {
                // The allowlist can be revoked after order creation. Recheck while the payment
                // transaction is still open, immediately before any beans are credited.
                legendaryService.requireEligible(order.getCustomerId());
                goldBeanService.completeLegendaryBankPurchase(
                        order.getTenantId(), order.getCustomerId(), order.getGoldBeanQuantity(), orderNo);
            } else {
                throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
            }
            stage = "order_update";
            LocalDateTime now = LocalDateTime.now();
            order.setStatus(PAID);
            order.setPaymentChannel("WECHAT_VIRTUAL");
            order.setTransactionId(transactionId);
            order.setPaidAt(now);
            order.setPaymentNotifyAt(now);
            order.setUpdatedBy(order.getCustomerId());
            order.setUpdatedAt(now);
            orderMapper.updateById(order);
            if (REGISTRATION_TYPE.equals(order.getOrderType())
                    && REFERRER.equals(order.getRegistrationFeeRecipient())) {
                stage = "referrer_settlement";
                initiateReferralSettlement(order);
            }
        } catch (BusinessException exception) {
            log.warn(
                    "Virtual payment callback rejected: stage={}, orderNo={}, errorCode={}",
                    stage,
                    orderNo,
                    exception.getErrorCode().code());
            throw exception;
        } catch (Exception exception) {
            log.warn(
                    "Virtual payment callback failed: stage={}, orderNo={}, exceptionType={}",
                    stage,
                    orderNo,
                    exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
        }
    }

    /** Handles the verified standard WeChat Pay callback for a referral registration. */
    @Transactional
    public void handleWechatPayment(Transaction transaction) {
        TenantContext.executeReadWithoutTenant(() -> {
            handleWechatPaymentWithoutTenant(transaction);
            return null;
        });
    }

    private void handleWechatPaymentWithoutTenant(Transaction transaction) {
        if (transaction == null || !StringUtils.hasText(transaction.getOutTradeNo())
                || !transaction.getOutTradeNo().startsWith(REGISTRATION_ORDER_PREFIX)) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_ORDER_NOT_FOUND);
        }
        String orderNo = transaction.getOutTradeNo();
        GoldBeanOrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<GoldBeanOrderEntity>()
                .eq(GoldBeanOrderEntity::getOrderNo, orderNo)
                .eq(GoldBeanOrderEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (order == null) throw new BusinessException(ErrorCode.GOLD_BEAN_ORDER_NOT_FOUND);
        if (PAID.equals(order.getStatus())) return;
        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        Integer amount = transaction.getAmount() == null ? null : transaction.getAmount().getTotal();
        if (!REGISTRATION_TYPE.equals(order.getOrderType())
                || !REFERRER.equals(order.getRegistrationFeeRecipient())
                || !realWechatPaymentConfigured()
                || !pay.appId().equals(transaction.getAppid())
                || !pay.merchantId().equals(transaction.getMchid())
                || transaction.getTradeState() != Transaction.TradeStateEnum.SUCCESS
                || amount == null || amount.intValue() != paymentAmountCent(order)
                || !StringUtils.hasText(transaction.getTransactionId())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_PAYMENT_NOT_CONFIGURED);
        }
        GoldBeanOrderEntity duplicate = orderMapper.selectOne(new LambdaQueryWrapper<GoldBeanOrderEntity>()
                .eq(GoldBeanOrderEntity::getTransactionId, transaction.getTransactionId())
                .ne(GoldBeanOrderEntity::getOrderNo, orderNo)
                .eq(GoldBeanOrderEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (duplicate != null) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
        }

        goldBeanService.completeRegistration(
                order.getTenantId(), order.getCustomerId(), order.getRegistrationReferralCode(),
                order.getRegistrationReferrerId(), order.getRegistrationCity(), REFERRER, orderNo);
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(PAID);
        order.setPaymentChannel("WECHAT_JSAPI");
        order.setTransactionId(transaction.getTransactionId());
        order.setPaidAt(now);
        order.setPaymentNotifyAt(now);
        order.setSettlementStatus(SETTLEMENT_PENDING);
        order.setUpdatedBy(order.getCustomerId());
        order.setUpdatedAt(now);
        orderMapper.updateById(order);
        initiateReferralSettlement(order);
    }

    private void initiateReferralSettlement(GoldBeanOrderEntity order) {
        if (!PAID.equals(order.getStatus()) || !REFERRER.equals(order.getRegistrationFeeRecipient())
                || SETTLEMENT_SETTLED.equals(order.getSettlementStatus())
                || SETTLEMENT_FAILED.equals(order.getSettlementStatus())) {
            return;
        }
        if (!referralTransferEnabled || !StringUtils.hasText(referralTransferSceneId)
                || !realWechatPaymentConfigured()) {
            scheduleSettlementRetry(order, "推荐人商家转账能力暂不可用，配置恢复后自动重试");
            return;
        }
        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        WeChatUserBindingEntity binding = activeBinding(order.getTenantId(), order.getRegistrationReferrerId(), pay.appId());
        if (binding == null || !StringUtils.hasText(binding.getOpenid())) {
            scheduleSettlementRetry(order, "推荐人尚未完成微信收款绑定，绑定后自动重试");
            return;
        }
        String outBillNo = StringUtils.hasText(order.getSettlementDetailNo())
                ? order.getSettlementDetailNo()
                : order.getOrderNo() + "T";
        if (!StringUtils.hasText(order.getSettlementDetailNo())) {
            order.setSettlementDetailNo(outBillNo);
            order.setSettlementStatus(SETTLEMENT_PENDING);
            order.setSettlementLastCheckedAt(LocalDateTime.now());
            order.setSettlementNextRetryAt(LocalDateTime.now().plusSeconds(referralSettlementRetrySeconds));
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
        }
        if (StringUtils.hasText(order.getSettlementBatchNo())) {
            syncReferralSettlement(order.getOrderNo());
            return;
        }
        try {
            String authorizationId = referralAuthorizationService.activeAuthorizationId(
                    order.getTenantId(), order.getRegistrationReferrerId(), binding.getOpenid());
            WeChatMerchantTransferClient.TransferBill response = StringUtils.hasText(authorizationId)
                    ? merchantTransferClient.transferAfterAuthorization(
                            outBillNo,
                            authorizationId,
                            order.getAmountCent().longValue(),
                            referralTransferSceneId,
                            referralTransferNotifyUrl,
                            referralTransferUserRecvPerception,
                            referralTransferJobType,
                            referralTransferRewardDescription)
                    : merchantTransferClient.initiate(
                            outBillNo,
                            binding.getOpenid(),
                            order.getAmountCent().longValue(),
                            referralTransferSceneId,
                            referralTransferNotifyUrl,
                            referralTransferUserRecvPerception,
                            referralTransferJobType,
                            referralTransferRewardDescription);
            applyTransferBill(order, response);
        } catch (RuntimeException exception) {
            scheduleSettlementRetry(order, "微信商家转账请求结果待查，系统将使用原单号查单");
            log.warn(
                    "Referral settlement initiation requires reconciliation: orderNo={}, exceptionType={}",
                    order.getOrderNo(),
                    exception.getClass().getSimpleName());
        }
    }

    @Transactional
    public void syncReferralSettlement(String orderNo) {
        GoldBeanOrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<GoldBeanOrderEntity>()
                .eq(GoldBeanOrderEntity::getOrderNo, orderNo)
                .eq(GoldBeanOrderEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (order == null || !PAID.equals(order.getStatus())
                || !REFERRER.equals(order.getRegistrationFeeRecipient())
                || SETTLEMENT_SETTLED.equals(order.getSettlementStatus())
                || SETTLEMENT_FAILED.equals(order.getSettlementStatus())) {
            return;
        }
        if (!StringUtils.hasText(order.getSettlementDetailNo())) {
            initiateReferralSettlement(order);
            return;
        }
        if (!StringUtils.hasText(order.getSettlementBatchNo())) {
            if (order.getSettlementLastCheckedAt() == null) {
                initiateReferralSettlement(order);
                return;
            }
            try {
                WeChatMerchantTransferClient.TransferBill existing = merchantTransferClient.query(
                        order.getSettlementDetailNo());
                if (StringUtils.hasText(existing.state()) || StringUtils.hasText(existing.transferBillNo())) {
                    applyTransferBill(order, existing);
                    if (confirmationPackageMissing(order)) {
                        recoverReferralSettlementPackage(order);
                    }
                } else {
                    scheduleSettlementRetry(order, "微信转账单尚未返回明确状态");
                }
            } catch (WeChatMerchantTransferClient.TransferBillNotFoundException exception) {
                // A failed create may leave the persisted out_bill_no without a bill at WeChat.
                // Re-submit the same number after NOT_FOUND; the number remains the idempotency key.
                scheduleSettlementRetry(order, "微信转账单不存在，系统将使用原单号重新发起");
                initiateReferralSettlement(order);
            } catch (RuntimeException exception) {
                scheduleSettlementRetry(order, "微信转账单查询暂时失败，系统将稍后重试");
            }
            return;
        }
        try {
            WeChatMerchantTransferClient.TransferBill bill = merchantTransferClient.query(
                    order.getSettlementDetailNo());
            if (bill == null || (!StringUtils.hasText(bill.state()) && !StringUtils.hasText(bill.transferBillNo()))) {
                scheduleSettlementRetry(order, "微信转账单尚未返回明确状态");
            } else {
                applyTransferBill(order, bill);
                if (confirmationPackageMissing(order)) {
                    recoverReferralSettlementPackage(order);
                }
            }
        } catch (RuntimeException exception) {
            scheduleSettlementRetry(order, "微信转账单查询暂时失败，系统将稍后重试");
        }
    }

    /**
     * The package used by requestMerchantTransfer is returned only by the create response. If an
     * earlier process lost that response after WeChat accepted the same out_bill_no, safely repeat
     * the create request with the persisted idempotency key and original parameters so WeChat can
     * return the confirmation package without creating a second transfer.
     */
    private void recoverReferralSettlementPackage(GoldBeanOrderEntity order) {
        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        WeChatUserBindingEntity binding = activeBinding(order.getTenantId(), order.getRegistrationReferrerId(), pay.appId());
        if (binding == null || !StringUtils.hasText(binding.getOpenid())) {
            scheduleSettlementRetry(order, "推荐人尚未完成微信收款绑定，绑定后自动重试");
            return;
        }
        try {
            WeChatMerchantTransferClient.TransferBill response = merchantTransferClient.initiate(
                    order.getSettlementDetailNo(),
                    binding.getOpenid(),
                    order.getAmountCent().longValue(),
                    referralTransferSceneId,
                    referralTransferNotifyUrl,
                    referralTransferUserRecvPerception,
                    referralTransferJobType,
                    referralTransferRewardDescription);
            if (response == null
                    || (!StringUtils.hasText(response.state())
                            && !StringUtils.hasText(response.transferBillNo())
                            && !StringUtils.hasText(response.packageInfo()))) {
                scheduleSettlementRetry(order, "微信转账单已受理，收款确认参数尚未返回，系统将继续恢复");
            } else {
                applyTransferBill(order, response);
            }
        } catch (RuntimeException exception) {
            scheduleSettlementRetry(order, "微信转账单已进入待确认状态，收款确认参数恢复请求待重试");
            log.warn(
                    "Referral settlement confirmation package recovery requires retry: orderNo={}, exceptionType={}",
                    order.getOrderNo(),
                    exception.getClass().getSimpleName());
        }
    }

    private boolean confirmationPackageMissing(GoldBeanOrderEntity order) {
        String state = order.getSettlementWechatState();
        return ("WAIT_USER_CONFIRM".equals(state) || "TRANSFERING".equals(state))
                && !StringUtils.hasText(order.getSettlementPackageInfo());
    }

    /** Reconciles pending referral payouts from a scheduler or a referrer refresh action. */
    public void recoverPendingReferralSettlements() {
        if (!referralTransferEnabled || !StringUtils.hasText(referralTransferSceneId)
                || !realWechatPaymentConfigured()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<GoldBeanOrderEntity> candidates = TenantContext.executeReadWithoutTenant(
                () -> orderMapper.selectList(new LambdaQueryWrapper<GoldBeanOrderEntity>()
                        .eq(GoldBeanOrderEntity::getStatus, PAID)
                        .eq(GoldBeanOrderEntity::getRegistrationFeeRecipient, REFERRER)
                        .eq(GoldBeanOrderEntity::getSettlementStatus, SETTLEMENT_PENDING)
                        .eq(GoldBeanOrderEntity::getDeleted, 0)
                        .and(query -> query.isNull(GoldBeanOrderEntity::getSettlementNextRetryAt)
                                .or()
                                .le(GoldBeanOrderEntity::getSettlementNextRetryAt, now))
                        .orderByAsc(GoldBeanOrderEntity::getCreatedAt)
                        .last("LIMIT 50")));
        for (GoldBeanOrderEntity candidate : candidates) {
            try {
                settlementTransactionTemplate.executeWithoutResult(transactionStatus ->
                        TenantContext.execute(candidate.getTenantId(), () -> syncReferralSettlement(candidate.getOrderNo())));
            } catch (RuntimeException exception) {
                log.warn(
                        "Referral settlement recovery failed: orderNo={}, exceptionType={}",
                        candidate.getOrderNo(),
                        exception.getClass().getSimpleName());
            }
        }
    }

    @Transactional
    public List<GoldBeanReferralSettlementVo> referralSettlements() {
        CurrentPrincipal current = requireCustomer();
        return orderMapper.selectList(new LambdaQueryWrapper<GoldBeanOrderEntity>()
                        .eq(GoldBeanOrderEntity::getTenantId, current.tenantId())
                        .eq(GoldBeanOrderEntity::getRegistrationReferrerId, current.userId())
                        .eq(GoldBeanOrderEntity::getRegistrationFeeRecipient, REFERRER)
                        .eq(GoldBeanOrderEntity::getStatus, PAID)
                        .in(GoldBeanOrderEntity::getSettlementStatus,
                                SETTLEMENT_PENDING, SETTLEMENT_SETTLED, SETTLEMENT_FAILED)
                        .eq(GoldBeanOrderEntity::getDeleted, 0)
                        .orderByDesc(GoldBeanOrderEntity::getCreatedAt)
                        .last("LIMIT 20"))
                .stream()
                .map(this::toReferralSettlementVo)
                .toList();
    }

    @Transactional
    public GoldBeanReferralSettlementVo syncReferralSettlementForReferrer(String orderNo) {
        CurrentPrincipal current = requireCustomer();
        GoldBeanOrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<GoldBeanOrderEntity>()
                .eq(GoldBeanOrderEntity::getTenantId, current.tenantId())
                .eq(GoldBeanOrderEntity::getRegistrationReferrerId, current.userId())
                .eq(GoldBeanOrderEntity::getRegistrationFeeRecipient, REFERRER)
                .eq(GoldBeanOrderEntity::getOrderNo, orderNo == null ? "" : orderNo.trim())
                .eq(GoldBeanOrderEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (order == null) throw new BusinessException(ErrorCode.GOLD_BEAN_ORDER_NOT_FOUND);
        if (PAID.equals(order.getStatus()) && SETTLEMENT_PENDING.equals(order.getSettlementStatus())) {
            reconcileReferralSettlement(order);
            GoldBeanOrderEntity refreshed = orderMapper.selectOne(new LambdaQueryWrapper<GoldBeanOrderEntity>()
                    .eq(GoldBeanOrderEntity::getTenantId, current.tenantId())
                    .eq(GoldBeanOrderEntity::getRegistrationReferrerId, current.userId())
                    .eq(GoldBeanOrderEntity::getRegistrationFeeRecipient, REFERRER)
                    .eq(GoldBeanOrderEntity::getOrderNo, orderNo == null ? "" : orderNo.trim())
                    .eq(GoldBeanOrderEntity::getDeleted, 0)
                    .last("LIMIT 1"));
            if (refreshed != null) order = refreshed;
        }
        return toReferralSettlementVo(order);
    }

    private void reconcileReferralSettlement(GoldBeanOrderEntity order) {
        if (!StringUtils.hasText(order.getSettlementDetailNo())) {
            initiateReferralSettlement(order);
        } else {
            syncReferralSettlement(order.getOrderNo());
        }
    }

    private void applyTransferBill(GoldBeanOrderEntity order, WeChatMerchantTransferClient.TransferBill bill) {
        LocalDateTime now = LocalDateTime.now();
        String state = bill.state() == null ? "" : bill.state().trim().toUpperCase(Locale.ROOT);
        if (StringUtils.hasText(bill.transferBillNo())) {
            order.setSettlementBatchNo(bill.transferBillNo());
        }
        if (StringUtils.hasText(state)) order.setSettlementWechatState(state);
        if (StringUtils.hasText(bill.packageInfo())) order.setSettlementPackageInfo(bill.packageInfo());
        order.setSettlementLastCheckedAt(now);
        if ("SUCCESS".equals(state)) {
            order.setSettlementStatus(SETTLEMENT_SETTLED);
            order.setSettledAt(now);
            order.setSettlementFailureReason(null);
            order.setSettlementNextRetryAt(null);
        } else if ("FAIL".equals(state) || "CLOSED".equals(state) || "CANCELLED".equals(state)) {
            order.setSettlementStatus(SETTLEMENT_FAILED);
            order.setSettlementFailureReason(transferFailureReason(bill.failReason()));
            order.setSettlementNextRetryAt(null);
        } else {
            order.setSettlementStatus(SETTLEMENT_PENDING);
            order.setSettlementFailureReason(null);
            order.setSettlementNextRetryAt(now.plusSeconds(referralSettlementRetrySeconds));
        }
        order.setUpdatedAt(now);
        orderMapper.updateById(order);
    }

    private void scheduleSettlementRetry(GoldBeanOrderEntity order, String reason) {
        LocalDateTime now = LocalDateTime.now();
        order.setSettlementStatus(SETTLEMENT_PENDING);
        order.setSettlementFailureReason(reason);
        order.setSettlementLastCheckedAt(now);
        order.setSettlementNextRetryAt(now.plusSeconds(referralSettlementRetrySeconds));
        order.setUpdatedAt(now);
        orderMapper.updateById(order);
    }

    private String transferFailureReason(String reason) {
        if (!StringUtils.hasText(reason)) return "推荐人微信收款未成功，需平台处理";
        String normalized = reason.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    private GoldBeanReferralSettlementVo toReferralSettlementVo(GoldBeanOrderEntity order) {
        String state = order.getSettlementWechatState();
        boolean confirmation = ("WAIT_USER_CONFIRM".equals(state) || "TRANSFERING".equals(state))
                && StringUtils.hasText(order.getSettlementPackageInfo());
        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        return new GoldBeanReferralSettlementVo(
                order.getOrderNo(),
                order.getAmountCent(),
                order.getSettlementStatus(),
                state,
                confirmation ? pay.merchantId() : null,
                confirmation ? pay.appId() : null,
                confirmation,
                confirmation ? order.getSettlementPackageInfo() : null,
                order.getCreatedAt(),
                order.getSettledAt(),
                order.getSettlementFailureReason());
    }

    private GoldBeanAccountEntity findActiveReferrer(long tenantId, long referredUserId, String referralCode) {
        GoldBeanAccountEntity referrer = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, tenantId)
                .eq(GoldBeanAccountEntity::getReferralCode, referralCode)
                .eq(GoldBeanAccountEntity::getStatus, "ACTIVE")
                .eq(GoldBeanAccountEntity::getRegistrationFeeStatus, "PAID")
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (referrer == null || referrer.getUserId() == null || referrer.getUserId() == referredUserId) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_INVALID);
        }
        return referrer;
    }

    private void ensureReferralPaymentConfigured(long tenantId, Long referrerId) {
        if (referrerId == null || !referralTransferEnabled || !StringUtils.hasText(referralTransferSceneId)
                || !realWechatPaymentConfigured()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_PAYMENT_NOT_CONFIGURED);
        }
        GoldBeanAccountEntity referrer = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, tenantId)
                .eq(GoldBeanAccountEntity::getUserId, referrerId)
                .eq(GoldBeanAccountEntity::getStatus, "ACTIVE")
                .eq(GoldBeanAccountEntity::getRegistrationFeeStatus, "PAID")
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (referrer == null) throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRAL_INVALID);
        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        WeChatUserBindingEntity binding = activeBinding(tenantId, referrerId, pay.appId());
        if (binding == null || !StringUtils.hasText(binding.getOpenid())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_REFERRER_BINDING_REQUIRED);
        }
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

    private boolean realWechatPaymentConfigured() {
        return goldBeanService.available()
                && properties.paymentEnabled()
                && membershipProperties.wechatPay().configured()
                && weChatPayClient.configured();
    }

    public boolean isGoldBeanNotification(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String orderNo = text(root, "outTradeNo", "OutTradeNo", "out_trade_no");
            if (!StringUtils.hasText(orderNo)) {
                String payloadText = payloadText(root);
                if (StringUtils.hasText(payloadText)) {
                    orderNo = text(objectMapper.readTree(payloadText), "OutTradeNo", "outTradeNo", "out_trade_no");
                }
            }
            return isGoldBeanOrder(orderNo);
        } catch (Exception exception) {
            return false;
        }
    }

    private CurrentPrincipal requireCustomer() {
        if (!goldBeanService.available() || !properties.paymentEnabled()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_NOT_CONFIGURED);
        }
        CurrentPrincipal current = CurrentUser.require();
        if (!"CUSTOMER".equals(current.workbench())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        return current;
    }

    private int registrationFeeCent(String recipient) {
        return REFERRER.equals(recipient)
                ? properties.registrationFeeCent()
                : properties.platformRegistrationFeeCent();
    }

    private void requirePaymentProduct(String productId) {
        requireCustomer();
        if (!virtualPayClient.configuredFor(productId)) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_NOT_CONFIGURED);
        }
    }

    private GoldBeanOrderEntity newOrder(
            CurrentPrincipal current, String prefix, String orderType, LocalDateTime now, String orderNo) {
        GoldBeanOrderEntity order = new GoldBeanOrderEntity();
        order.setId(IdWorker.getId());
        order.setTenantId(current.tenantId());
        order.setCustomerId(current.userId());
        order.setOrderNo(orderNo == null ? prefix + IdWorker.getId() : orderNo);
        order.setOrderType(orderType);
        order.setStatus(PENDING);
        order.setCreatedBy(current.userId());
        order.setCreatedAt(now);
        order.setUpdatedBy(current.userId());
        order.setUpdatedAt(now);
        order.setDeleted(0);
        order.setVersion(0);
        return order;
    }

    private GoldBeanOrderEntity ownedOrder(String orderNo) {
        return ownedOrder(orderNo, false);
    }

    private GoldBeanOrderEntity ownedOrderForUpdate(String orderNo) {
        return ownedOrder(orderNo, true);
    }

    private GoldBeanOrderEntity ownedOrder(String orderNo, boolean lock) {
        CurrentPrincipal current = requireCustomer();
        if (!StringUtils.hasText(orderNo)) throw new BusinessException(ErrorCode.GOLD_BEAN_ORDER_NOT_FOUND);
        LambdaQueryWrapper<GoldBeanOrderEntity> query = new LambdaQueryWrapper<GoldBeanOrderEntity>()
                .eq(GoldBeanOrderEntity::getOrderNo, orderNo.trim())
                .eq(GoldBeanOrderEntity::getTenantId, current.tenantId())
                .eq(GoldBeanOrderEntity::getCustomerId, current.userId())
                .eq(GoldBeanOrderEntity::getDeleted, 0);
        query.last(lock ? "LIMIT 1 FOR UPDATE" : "LIMIT 1");
        GoldBeanOrderEntity order = orderMapper.selectOne(query);
        if (order == null) throw new BusinessException(ErrorCode.GOLD_BEAN_ORDER_NOT_FOUND);
        return order;
    }

    private void closeIfExpired(GoldBeanOrderEntity order) {
        if (!PENDING.equals(order.getStatus())
                || order.getExpiresAt() == null
                || LocalDateTime.now().isBefore(order.getExpiresAt())) return;
        LocalDateTime now = LocalDateTime.now();
        order.setStatus(CLOSED);
        order.setPlatformSlotKey(null);
        if (REFERRER.equals(order.getRegistrationFeeRecipient())) {
            order.setSettlementStatus(SETTLEMENT_NOT_REQUIRED);
        }
        order.setUpdatedBy(order.getCustomerId());
        order.setUpdatedAt(now);
        orderMapper.updateById(order);
        platformInviteService.release(order.getPlatformInviteId(), order.getOrderNo(), order.getCustomerId());
    }

    private void closeExpiredPlatformRegistrationOrders() {
        LocalDateTime now = LocalDateTime.now();
        TenantContext.executeReadWithoutTenant(() -> {
            orderMapper.selectList(new LambdaQueryWrapper<GoldBeanOrderEntity>()
                            .eq(GoldBeanOrderEntity::getPlatformSlotKey, GoldBeanPlatformInviteService.PLATFORM_ROOT_SLOT)
                            .eq(GoldBeanOrderEntity::getStatus, PENDING)
                            .eq(GoldBeanOrderEntity::getDeleted, 0)
                            .le(GoldBeanOrderEntity::getExpiresAt, now)
                            .last("LIMIT 20 FOR UPDATE"))
                    .forEach(order -> {
                        order.setStatus(CLOSED);
                        order.setPlatformSlotKey(null);
                        order.setUpdatedBy(order.getCustomerId());
                        order.setUpdatedAt(now);
                        orderMapper.updateById(order);
                        platformInviteService.release(
                                order.getPlatformInviteId(), order.getOrderNo(), order.getCustomerId());
                    });
            return null;
        });
    }

    private GoldBeanOrderVo toVo(GoldBeanOrderEntity order) {
        return new GoldBeanOrderVo(
                order.getOrderNo(),
                order.getOrderType(),
                order.getStatus(),
                order.getAmountCent(),
                paymentAmountCent(order),
                order.getGoldBeanQuantity(),
                order.getRegistrationFeeRecipient(),
                properties.paymentEnabled(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getSettlementStatus());
    }

    /**
     * The virtual-payment goods quantity is an integer. Fractional bean orders are represented by
     * one virtual good priced at the complete payable amount, while integer orders retain the
     * historical quantity/unit-price representation.
     */
    private int paymentGoodsQuantity(GoldBeanOrderEntity order) {
        if (REGISTRATION_TYPE.equals(order.getOrderType())) return 1;
        BigDecimal beanQuantity = GoldBeanAmounts.normalize(order.getGoldBeanQuantity());
        try {
            return beanQuantity.stripTrailingZeros().scale() <= 0
                    ? beanQuantity.intValueExact()
                    : 1;
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
        }
    }

    private int paymentGoodsPrice(GoldBeanOrderEntity order) {
        if (REGISTRATION_TYPE.equals(order.getOrderType())) return paymentAmountCent(order);
        BigDecimal beanQuantity = GoldBeanAmounts.normalize(order.getGoldBeanQuantity());
        if (beanQuantity.stripTrailingZeros().scale() > 0) return paymentAmountCent(order);
        int quantity = paymentGoodsQuantity(order);
        if (quantity <= 0) throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_UNAVAILABLE);
        return paymentAmountCent(order) / quantity;
    }

    /** Old rows predate the buyer/settlement split and are intentionally treated as historical. */
    private int paymentAmountCent(GoldBeanOrderEntity order) {
        return order.getPaymentAmountCent() == null
                ? order.getAmountCent()
                : order.getPaymentAmountCent();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCity(String value) {
        return TextEncodingUtils.repairUtf8Mojibake(value == null ? "" : value.trim());
    }

    private String normalizeTransferText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private boolean isGoldBeanOrder(String orderNo) {
        return orderNo != null
                && (orderNo.startsWith(REGISTRATION_ORDER_PREFIX) || orderNo.startsWith(PURCHASE_ORDER_PREFIX));
    }

    private boolean isSupportedVirtualPaymentEvent(String event, String eventType) {
        if ("xpay_goods_deliver_notify".equalsIgnoreCase(event)) {
            // Some virtual-payment console versions omit EventType on the goods-delivery push.
            // The content HMAC and the payment-field checks below remain mandatory.
            return !StringUtils.hasText(eventType) || "TRANSACTION.SUCCESS".equalsIgnoreCase(eventType);
        }
        if ("minigame_game_pay_goods_deliver_notify".equalsIgnoreCase(event)) {
            return "event".equalsIgnoreCase(eventType) || "TRANSACTION.SUCCESS".equalsIgnoreCase(eventType);
        }
        return false;
    }

    private boolean hasDirectGoodsDeliveryFields(JsonNode root) {
        return root != null
                && StringUtils.hasText(text(root, "OutTradeNo", "outTradeNo", "out_trade_no"))
                && StringUtils.hasText(text(root, "OpenId", "openid"))
                && child(root, "GoodsInfo", "goodsInfo") != null;
    }

    private String text(JsonNode node, String... names) {
        if (node == null) return null;
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull() && StringUtils.hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return null;
    }

    private JsonNode child(JsonNode node, String... names) {
        if (node == null) return null;
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) return value;
        }
        return null;
    }

    private String payloadText(JsonNode root) {
        if (root == null) return null;
        JsonNode payload = root.get("payload");
        if (payload == null || payload.isNull()) payload = root.get("Payload");
        if (payload == null || payload.isNull()) return null;
        return payload.isTextual() ? payload.textValue() : payload.toString();
    }

    private int integer(JsonNode node, String... names) {
        if (node == null) return -1;
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value == null || value.isNull()) continue;
            if (value.isIntegralNumber()) return value.asInt(-1);
            try {
                return Integer.parseInt(value.asText());
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }
}
