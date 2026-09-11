package com.rayk.health.goldbean.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.common.util.TextEncodingUtils;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.dto.BuyGoldBeanTradeRequest;
import com.rayk.health.goldbean.dto.CreateGoldBeanTradeListingRequest;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanTradeEntity;
import com.rayk.health.goldbean.entity.GoldBeanTradeListingEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanTradeListingMapper;
import com.rayk.health.goldbean.mapper.GoldBeanTradeMapper;
import com.rayk.health.goldbean.vo.GoldBeanTradeListingVo;
import com.rayk.health.goldbean.vo.GoldBeanTradePayoutVo;
import com.rayk.health.goldbean.vo.GoldBeanTradeVo;
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
import com.wechat.pay.java.service.transferbatch.model.GetTransferDetailByOutNoRequest;
import com.wechat.pay.java.service.transferbatch.model.InitiateBatchTransferRequest;
import com.wechat.pay.java.service.transferbatch.model.InitiateBatchTransferResponse;
import com.wechat.pay.java.service.transferbatch.model.TransferDetailEntity;
import com.wechat.pay.java.service.transferbatch.model.TransferDetailInput;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.rayk.health.goldbean.util.GoldBeanAmounts;

/** Virtual-goods payment in, merchant transfer to the seller, then gold-bean delivery. */
@Service
public class GoldBeanTradeService {
    private static final Logger log = LoggerFactory.getLogger(GoldBeanTradeService.class);
    private static final String TRADE_ORDER_PREFIX = "GBT";
    private static final String OPEN = "OPEN";
    private static final String FILLED = "FILLED";
    private static final String CANCELLED = "CANCELLED";
    private static final String PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";
    private static final String TRANSFER_PENDING = "TRANSFER_PENDING";
    private static final String COMPLETED = "COMPLETED";
    private static final String REFUND_REQUIRED = "REFUND_REQUIRED";
    private static final String PAID = "PAID";
    private static final String TRADING = "TRADING";
    private static final String DIGITAL_BANK = "DIGITAL_BANK";
    private static final String BUYER_CREDIT_SPLIT = "SPLIT";
    private static final String BUYER_CREDIT_DIGITAL_BANK = "DIGITAL_BANK";
    private static final long FIXED_UNIT_PRICE_CENT = 100L;

    private final GoldBeanApplicationService goldBeanService;
    private final GoldBeanLegendaryService legendaryService;
    private final GoldBeanAccountMapper accountMapper;
    private final GoldBeanTradeListingMapper listingMapper;
    private final GoldBeanTradeMapper tradeMapper;
    private final WeChatUserBindingMapper bindingMapper;
    private final WeChatPayClient weChatPayClient;
    private final WeChatMerchantTransferClient merchantTransferClient;
    private final GoldBeanReferralAuthorizationService referralAuthorizationService;
    private final MembershipProperties membershipProperties;
    private final GoldBeanProperties goldBeanProperties;
    private final WeChatVirtualPayClient virtualPayClient;
    private final ObjectMapper objectMapper;
    private final boolean paymentEnabled;
    private final String transferSceneId;
    private final String transferNotifyUrl;
    private final String transferUserRecvPerception;
    private final String transferJobType;
    private final String transferRewardDescription;
    private final int settlementRetrySeconds;

    public GoldBeanTradeService(
            GoldBeanApplicationService goldBeanService,
            GoldBeanLegendaryService legendaryService,
            GoldBeanAccountMapper accountMapper,
            GoldBeanTradeListingMapper listingMapper,
            GoldBeanTradeMapper tradeMapper,
            WeChatUserBindingMapper bindingMapper,
            WeChatPayClient weChatPayClient,
            WeChatMerchantTransferClient merchantTransferClient,
            GoldBeanReferralAuthorizationService referralAuthorizationService,
            MembershipProperties membershipProperties,
            GoldBeanProperties goldBeanProperties,
            WeChatVirtualPayClient virtualPayClient,
            ObjectMapper objectMapper,
            @Value("${rayk.gold-bean.trade-payment-enabled:false}") boolean paymentEnabled,
            @Value("${rayk.gold-bean.trade-transfer-scene-id:}") String transferSceneId,
            @Value("${rayk.gold-bean.trade-transfer-notify-url:}") String transferNotifyUrl,
            @Value("${rayk.gold-bean.trade-transfer-user-recv-perception:劳务报酬}")
                    String transferUserRecvPerception,
            @Value("${rayk.gold-bean.trade-transfer-job-type:金豆交易服务}") String transferJobType,
            @Value("${rayk.gold-bean.trade-transfer-reward-description:金豆交易结算}")
                    String transferRewardDescription,
            @Value("${rayk.gold-bean.trade-settlement-retry-seconds:30}") int settlementRetrySeconds) {
        this.goldBeanService = goldBeanService;
        this.legendaryService = legendaryService;
        this.accountMapper = accountMapper;
        this.listingMapper = listingMapper;
        this.tradeMapper = tradeMapper;
        this.bindingMapper = bindingMapper;
        this.weChatPayClient = weChatPayClient;
        this.merchantTransferClient = merchantTransferClient;
        this.referralAuthorizationService = referralAuthorizationService;
        this.membershipProperties = membershipProperties;
        this.goldBeanProperties = goldBeanProperties;
        this.virtualPayClient = virtualPayClient;
        this.objectMapper = objectMapper;
        this.paymentEnabled = paymentEnabled;
        this.transferSceneId = transferSceneId == null ? "" : transferSceneId.trim();
        this.transferNotifyUrl = transferNotifyUrl == null ? "" : transferNotifyUrl.trim();
        this.transferUserRecvPerception = normalizeTransferText(transferUserRecvPerception, "劳务报酬");
        this.transferJobType = normalizeTransferText(transferJobType, "金豆交易服务");
        this.transferRewardDescription = normalizeTransferText(transferRewardDescription, "金豆交易结算");
        this.settlementRetrySeconds = Math.max(10, Math.min(settlementRetrySeconds, 3600));
    }

    @Transactional(readOnly = true)
    public PageResponse<GoldBeanTradeListingVo> market(long pageNumber, long pageSize) {
        CurrentPrincipal current = requireCustomer();
        GoldBeanAccountEntity buyer = currentAccount(current);
        requireRegistered(buyer);
        boolean legendaryBuyer = legendaryService.isEligible(current.userId());
        String visibleBucket = legendaryBuyer ? DIGITAL_BANK : TRADING;
        LambdaQueryWrapper<GoldBeanTradeListingEntity> query = new LambdaQueryWrapper<GoldBeanTradeListingEntity>()
                .eq(GoldBeanTradeListingEntity::getTenantId, current.tenantId())
                .eq(GoldBeanTradeListingEntity::getStatus, OPEN)
                .eq(GoldBeanTradeListingEntity::getDeleted, 0)
                .gt(GoldBeanTradeListingEntity::getRemainingQuantity, 0)
                .eq(GoldBeanTradeListingEntity::getBucket, visibleBucket)
                .orderByAsc(GoldBeanTradeListingEntity::getUnitPriceCent)
                .orderByAsc(GoldBeanTradeListingEntity::getCreatedAt);
        if (!legendaryBuyer) {
            query.eq(GoldBeanTradeListingEntity::getRegionCity, requireTradeRegion(buyer));
        }
        Page<GoldBeanTradeListingEntity> page = listingMapper.selectPage(
                new Page<>(normalizePage(pageNumber), normalizePageSize(pageSize)), query);
        List<GoldBeanTradeListingVo> records = page.getRecords().stream()
                .map(item -> toListing(item, current.userId()))
                .toList();
        return new PageResponse<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional(readOnly = true)
    public PageResponse<GoldBeanTradeListingVo> mine(long pageNumber, long pageSize) {
        CurrentPrincipal current = requireCustomer();
        Page<GoldBeanTradeListingEntity> page = listingMapper.selectPage(
                new Page<>(normalizePage(pageNumber), normalizePageSize(pageSize)),
                new LambdaQueryWrapper<GoldBeanTradeListingEntity>()
                        .eq(GoldBeanTradeListingEntity::getTenantId, current.tenantId())
                        .eq(GoldBeanTradeListingEntity::getSellerUserId, current.userId())
                        .eq(GoldBeanTradeListingEntity::getDeleted, 0)
                        .orderByDesc(GoldBeanTradeListingEntity::getCreatedAt));
        List<GoldBeanTradeListingVo> records = page.getRecords().stream()
                .map(item -> toListing(item, current.userId()))
                .toList();
        return new PageResponse<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional
    public GoldBeanTradeListingVo create(CreateGoldBeanTradeListingRequest request) {
        CurrentPrincipal current = requireCustomer();
        if (legendaryService.isEligible(current.userId())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LEGENDARY_SELL_FORBIDDEN);
        }
        BigDecimal quantity = GoldBeanAmounts.normalize(request == null ? null : request.quantity());
        if (!GoldBeanAmounts.positive(quantity)) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_AMOUNT_INVALID);
        }
        String bucket = normalizeBucket(request.normalizedBucket());
        GoldBeanAccountEntity seller = lockedAccount(current.tenantId(), current.userId());
        requireRegistered(seller);
        String sellerRegionCity = requireTradeRegion(seller);
        goldBeanService.settleForTrade(seller, current.userId());
        BigDecimal listed = openListings(current.tenantId(), current.userId());
        BigDecimal pending = pendingTradeQuantity(current.tenantId(), current.userId());
        BigDecimal digitalBankBalance = safeBalance(seller.getDigitalBankBalance());
        BigDecimal tradingBalance = safeBalance(seller.getTradingBalance());
        BigDecimal totalBalance = saturatingAdd(digitalBankBalance, tradingBalance);
        int tradeLimitPercent = seller.getTradeLimitPercent() == null
                ? 0
                : Math.max(0, Math.min(100, seller.getTradeLimitPercent()));
        // The cap is calculated from both buckets so splitting one account into two listing
        // sources cannot bypass the 50% limit.
        BigDecimal allowed = percentageOf(totalBalance, tradeLimitPercent);
        BigDecimal committed = saturatingAdd(listed, pending);
        BigDecimal remainingCapacity = committed.compareTo(allowed) >= 0
                ? GoldBeanAmounts.ZERO
                : allowed.subtract(committed);
        BigDecimal sourceBalance = DIGITAL_BANK.equals(bucket) ? digitalBankBalance : tradingBalance;
        BigDecimal sourceCommitted = saturatingAdd(
                openListings(current.tenantId(), current.userId(), bucket),
                pendingTradeQuantity(current.tenantId(), current.userId(), bucket));
        BigDecimal sourceAvailable = sourceCommitted.compareTo(sourceBalance) >= 0
                ? GoldBeanAmounts.ZERO
                : sourceBalance.subtract(sourceCommitted);
        if (quantity.compareTo(remainingCapacity) > 0) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LIMIT_EXCEEDED);
        }
        if (quantity.compareTo(sourceAvailable) > 0) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_BALANCE_INSUFFICIENT);
        }
        LocalDateTime now = LocalDateTime.now();
        GoldBeanTradeListingEntity listing = new GoldBeanTradeListingEntity();
        listing.setId(IdWorker.getId());
        listing.setTenantId(current.tenantId());
        listing.setSellerUserId(current.userId());
        listing.setBucket(bucket);
        listing.setRegionCity(sellerRegionCity);
        listing.setQuantity(quantity);
        listing.setRemainingQuantity(quantity);
        listing.setUnitPriceCent(FIXED_UNIT_PRICE_CENT);
        listing.setStatus(OPEN);
        audit(listing, current.userId(), now);
        listingMapper.insert(listing);
        return toListing(listing, current.userId());
    }

    /** Reserves a listing quantity and creates a WeChat virtual-goods payment order. */
    @Transactional
    public GoldBeanTradeVo buy(String listingId, BuyGoldBeanTradeRequest request) {
        CurrentPrincipal current = requireCustomer();
        BigDecimal quantity = GoldBeanAmounts.normalize(request == null ? null : request.quantity());
        if (!GoldBeanAmounts.positive(quantity)) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_AMOUNT_INVALID);
        }
        GoldBeanTradeListingEntity listing = lockedListing(listingId, current.tenantId());
        if (!OPEN.equals(listing.getStatus())
                || !GoldBeanAmounts.positive(listing.getRemainingQuantity())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_INVALID_STATUS);
        }
        if (Objects.equals(listing.getSellerUserId(), current.userId())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_SELF_BUY);
        }
        if (quantity.compareTo(listing.getRemainingQuantity()) > 0) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_INVALID_STATUS);
        }
        long amountCent;
        try {
            amountCent = GoldBeanAmounts.currencyCents(quantity, FIXED_UNIT_PRICE_CENT);
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_AMOUNT_INVALID);
        }
        GoldBeanAccountEntity buyer;
        GoldBeanAccountEntity seller;
        if (current.userId() < listing.getSellerUserId()) {
            buyer = lockedAccount(current.tenantId(), current.userId());
            seller = lockedAccount(current.tenantId(), listing.getSellerUserId());
        } else {
            seller = lockedAccount(current.tenantId(), listing.getSellerUserId());
            buyer = lockedAccount(current.tenantId(), current.userId());
        }
        requireRegistered(buyer);
        requireRegistered(seller);
        boolean legendaryBuyer = legendaryService.isEligible(buyer.getUserId());
        if (!legendaryBuyer && !sameTradeRegion(requireTradeRegion(buyer), listing.getRegionCity())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_REGION_MISMATCH);
        }
        goldBeanService.settleForTrade(buyer, current.userId());
        goldBeanService.settleForTrade(seller, seller.getUserId());
        String bucket = normalizeBucket(listing.getBucket());
        if (legendaryBuyer && !DIGITAL_BANK.equals(bucket)) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LEGENDARY_DIGITAL_ONLY);
        }
        if (!legendaryBuyer && DIGITAL_BANK.equals(bucket)) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_DIGITAL_BANK_LEGENDARY_ONLY);
        }
        String buyerCreditMode = legendaryBuyer ? BUYER_CREDIT_DIGITAL_BANK : BUYER_CREDIT_SPLIT;
        BigDecimal sourceBalance = DIGITAL_BANK.equals(bucket)
                ? safeBalance(seller.getDigitalBankBalance())
                : safeBalance(seller.getTradingBalance());
        BigDecimal pendingSource = pendingTradeQuantity(current.tenantId(), seller.getUserId(), bucket);
        if (sourceBalance.compareTo(quantity) < 0
                || pendingSource.compareTo(sourceBalance.subtract(quantity)) > 0) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_BALANCE_INSUFFICIENT);
        }

        LocalDateTime now = LocalDateTime.now();
        String tradeNo = "GBT" + IdWorker.getId();
        long paymentUnitPriceCent = goldBeanProperties.surchargedPaymentAmountCent(FIXED_UNIT_PRICE_CENT);
        long paymentAmount = goldBeanProperties.surchargedPaymentAmountCent(amountCent);
        GoldBeanTradeEntity trade = new GoldBeanTradeEntity();
        trade.setId(IdWorker.getId());
        trade.setTenantId(current.tenantId());
        trade.setTradeNo(tradeNo);
        trade.setListingId(listing.getId());
        trade.setSellerUserId(seller.getUserId());
        trade.setBuyerUserId(buyer.getUserId());
        trade.setBucket(bucket);
        trade.setBuyerCreditMode(buyerCreditMode);
        trade.setQuantity(quantity);
        trade.setUnitPriceCent(FIXED_UNIT_PRICE_CENT);
        trade.setTotalAmount(amountCent);
        trade.setPaymentUnitPriceCent(paymentUnitPriceCent);
        trade.setPaymentAmount(paymentAmount);
        trade.setStatus(PENDING_PAYMENT);
        trade.setExpiresAt(now.plusMinutes(30));
        audit(trade, current.userId(), now);
        tradeMapper.insert(trade);

        listing.setRemainingQuantity(GoldBeanAmounts.normalize(listing.getRemainingQuantity().subtract(quantity)));
        listing.setStatus(listing.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0 ? FILLED : OPEN);
        listing.setUpdatedBy(current.userId());
        listing.setUpdatedAt(now);
        listingMapper.updateById(listing);
        return toTrade(trade);
    }

    @Transactional
    public GoldBeanTradeVo trade(String tradeNo) {
        CurrentPrincipal current = requireCustomer();
        GoldBeanTradeEntity trade = ownedTrade(tradeNo, current, false);
        if (TRANSFER_PENDING.equals(trade.getStatus()) || PAYMENT_CONFIRMED.equals(trade.getStatus())) {
            syncTransfer(trade.getTradeNo());
            trade = ownedTrade(tradeNo, current, false);
        }
        return toTrade(trade);
    }

    /** Returns seller-side payouts, including a confirmation package when WeChat requires it. */
    @Transactional(readOnly = true)
    public List<GoldBeanTradePayoutVo> sellerPayouts() {
        CurrentPrincipal current = requireCustomer();
        return tradeMapper.selectList(new LambdaQueryWrapper<GoldBeanTradeEntity>()
                        .eq(GoldBeanTradeEntity::getTenantId, current.tenantId())
                        .eq(GoldBeanTradeEntity::getSellerUserId, current.userId())
                        .in(GoldBeanTradeEntity::getStatus,
                                PAYMENT_CONFIRMED, TRANSFER_PENDING, COMPLETED, REFUND_REQUIRED)
                        .eq(GoldBeanTradeEntity::getDeleted, 0)
                        .orderByDesc(GoldBeanTradeEntity::getCreatedAt)
                        .last("LIMIT 20"))
                .stream()
                .map(this::toPayout)
                .toList();
    }

    /** Lets the seller refresh one payout without exposing another user's trade number. */
    @Transactional
    public GoldBeanTradePayoutVo syncPayoutForSeller(String tradeNo) {
        CurrentPrincipal current = requireCustomer();
        GoldBeanTradeEntity trade = tradeMapper.selectOne(new LambdaQueryWrapper<GoldBeanTradeEntity>()
                .eq(GoldBeanTradeEntity::getTenantId, current.tenantId())
                .eq(GoldBeanTradeEntity::getSellerUserId, current.userId())
                .eq(GoldBeanTradeEntity::getTradeNo, tradeNo == null ? "" : tradeNo.trim())
                .eq(GoldBeanTradeEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (trade == null) throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_NOT_FOUND);
        if (PAYMENT_CONFIRMED.equals(trade.getStatus()) || TRANSFER_PENDING.equals(trade.getStatus())) {
            syncTransfer(trade.getTradeNo());
            GoldBeanTradeEntity refreshed = tradeMapper.selectOne(new LambdaQueryWrapper<GoldBeanTradeEntity>()
                    .eq(GoldBeanTradeEntity::getTenantId, current.tenantId())
                    .eq(GoldBeanTradeEntity::getSellerUserId, current.userId())
                    .eq(GoldBeanTradeEntity::getTradeNo, trade.getTradeNo())
                    .eq(GoldBeanTradeEntity::getDeleted, 0)
                    .last("LIMIT 1"));
            if (refreshed != null) trade = refreshed;
        }
        return toPayout(trade);
    }

    @Transactional
    public MembershipPaymentVo createWechatPayment(String tradeNo) {
        CurrentPrincipal current = requireCustomer();
        if (!tradePaymentConfigured()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_PAYMENT_NOT_CONFIGURED);
        }
        GoldBeanTradeEntity trade = ownedTrade(tradeNo, current, true);
        closeIfExpired(trade);
        if (!PENDING_PAYMENT.equals(trade.getStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_INVALID_STATUS);
        }
        return virtualPayClient.createGoodsPayment(
                trade.getTradeNo(),
                paymentGoodsQuantity(trade),
                goldBeanProperties.goldBeanProductId(),
                paymentGoodsPrice(trade),
                current.userId(),
                "gold-bean-trade:" + trade.getTradeNo());
    }

    @Transactional
    public GoldBeanTradeVo cancelTrade(String tradeNo) {
        CurrentPrincipal current = requireCustomer();
        GoldBeanTradeEntity trade = ownedTrade(tradeNo, current, true);
        closeIfExpired(trade);
        if (!PENDING_PAYMENT.equals(trade.getStatus())) return toTrade(trade);
        trade.setStatus(CANCELLED);
        trade.setUpdatedBy(current.userId());
        trade.setUpdatedAt(LocalDateTime.now());
        tradeMapper.updateById(trade);
        releaseListing(trade, current.userId());
        return toTrade(trade);
    }

    @Transactional
    public GoldBeanTradeListingVo cancel(String listingId) {
        CurrentPrincipal current = requireCustomer();
        GoldBeanTradeListingEntity listing = listingMapper.selectOne(new LambdaQueryWrapper<GoldBeanTradeListingEntity>()
                .eq(GoldBeanTradeListingEntity::getId, parseId(listingId))
                .eq(GoldBeanTradeListingEntity::getTenantId, current.tenantId())
                .eq(GoldBeanTradeListingEntity::getSellerUserId, current.userId())
                .eq(GoldBeanTradeListingEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (listing == null) throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_NOT_FOUND);
        if (!OPEN.equals(listing.getStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_INVALID_STATUS);
        }
        listing.setStatus(CANCELLED);
        listing.setUpdatedBy(current.userId());
        listing.setUpdatedAt(LocalDateTime.now());
        listingMapper.updateById(listing);
        return toListing(listing, current.userId());
    }

    /** Called by the verified standard WeChat Pay callback. Payment alone does not deliver beans. */
    @Transactional
    public void handleWechatPayment(Transaction transaction) {
        TenantContext.executeReadWithoutTenant(() -> {
            handleWechatPaymentWithoutTenant(transaction);
            return null;
        });
    }

    /** Returns whether a virtual-payment callback belongs to a member-to-member trade. */
    public boolean isGoldBeanTradeNotification(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String orderNo = text(root, "OutTradeNo", "outTradeNo", "out_trade_no");
            if (!StringUtils.hasText(orderNo)) {
                String payloadText = payloadText(root);
                if (StringUtils.hasText(payloadText)) {
                    orderNo = text(objectMapper.readTree(payloadText), "OutTradeNo", "outTradeNo", "out_trade_no");
                }
            }
            return StringUtils.hasText(orderNo) && orderNo.startsWith(TRADE_ORDER_PREFIX);
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Handles the verified WeChat virtual-goods delivery push for a trade order. */
    @Transactional
    public void handleVirtualPaymentNotification(String body, boolean messagePushAuthenticated) {
        String orderNo = "";
        String stage = "parse";
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null) throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_PAYMENT_NOT_CONFIGURED);
            stage = "event";
            String event = text(root, "event", "Event");
            String eventType = text(root, "eventType", "EventType", "event_type", "eventtype");
            if (!isSupportedVirtualPaymentEvent(event, eventType)) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_PAYMENT_NOT_CONFIGURED);
            }

            String payloadText = payloadText(root);
            boolean directMessagePush = !StringUtils.hasText(payloadText);
            stage = "event_signature";
            if (directMessagePush) {
                if (!messagePushAuthenticated || !hasDirectGoodsDeliveryFields(root)) {
                    throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_PAYMENT_NOT_CONFIGURED);
                }
            } else if (!virtualPayClient.verifyPaymentEventSignature(
                    event, payloadText, text(root, "payEventSig", "PayEventSig", "pay_event_sig"))) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_PAYMENT_NOT_CONFIGURED);
            }

            JsonNode payload = directMessagePush ? root : objectMapper.readTree(payloadText);
            String payloadOrderNo = text(payload, "OutTradeNo", "outTradeNo", "out_trade_no");
            orderNo = text(root, "OutTradeNo", "outTradeNo", "out_trade_no");
            if (!StringUtils.hasText(orderNo)) orderNo = payloadOrderNo;
            if (!StringUtils.hasText(orderNo) || !orderNo.startsWith(TRADE_ORDER_PREFIX)) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_NOT_FOUND);
            }
            if (StringUtils.hasText(payloadOrderNo) && !orderNo.equals(payloadOrderNo)) {
                throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_PAYMENT_NOT_CONFIGURED);
            }

            stage = "order_lookup";
            GoldBeanTradeEntity trade = tradeMapper.selectOne(new LambdaQueryWrapper<GoldBeanTradeEntity>()
                    .eq(GoldBeanTradeEntity::getTradeNo, orderNo)
                    .eq(GoldBeanTradeEntity::getDeleted, 0)
                    .last("LIMIT 1 FOR UPDATE"));
            if (trade == null) throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_NOT_FOUND);
            if (COMPLETED.equals(trade.getStatus())) return;
            // A callback can be retried after the first callback has already handed the
            // order to the payout reconciler. It is safe to acknowledge it idempotently.
            if (!PENDING_PAYMENT.equals(trade.getStatus())) {
                if (PAYMENT_CONFIRMED.equals(trade.getStatus()) || TRANSFER_PENDING.equals(trade.getStatus())) return;
                throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_INVALID_STATUS);
            }

            MembershipProperties.WeChatVirtualPayProperties pay = virtualPayClient.paymentProperties();
            int env = integer(payload, "Env", "env");
            if (env < 0) env = integer(root, "Env", "env");
            String openid = text(payload, "OpenId", "openid");
            JsonNode goods = child(payload, "GoodsInfo", "goodsInfo");
            JsonNode payment = child(payload, "PayInfo", "payInfo", "WeChatPayInfo", "weChatPayInfo");
            String productId = text(goods, "ProductId", "productId");
            int quantity = integer(goods, "Quantity", "quantity");
            int actualPrice = integer(goods, "ActualPrice", "actualPrice");
            String payloadTransactionId = text(payment, "TransactionId", "transactionId");
            String outerTransactionId = text(root, "TransactionId", "transactionId");
            String transactionId = StringUtils.hasText(payloadTransactionId)
                    ? payloadTransactionId
                    : outerTransactionId;
            String merchantCode = text(root, "MerchantCode", "merchantCode");
            if (!StringUtils.hasText(merchantCode)) merchantCode = text(payload, "MerchantCode", "merchantCode");

            String expectedProductId = goldBeanProperties.goldBeanProductId();
            boolean productConfigured = goldBeanProperties.paymentEnabled()
                    && virtualPayClient.configuredFor(expectedProductId);
            boolean envMatches = env < 0 || env == pay.env();
            boolean productMatches = expectedProductId.equals(productId);
            boolean quantityMatches = quantity == paymentGoodsQuantity(trade);
            boolean priceMatches = actualPrice == paymentGoodsPrice(trade);
            boolean openidPresent = StringUtils.hasText(openid);
            boolean outerTransactionMatches = !StringUtils.hasText(outerTransactionId)
                    || !StringUtils.hasText(payloadTransactionId)
                    || outerTransactionId.equals(payloadTransactionId);
            boolean merchantCodeMatches = !StringUtils.hasText(merchantCode)
                    || pay.merchantId().equals(merchantCode);
            if (!productConfigured || !envMatches || !productMatches || !quantityMatches || !priceMatches
                    || !openidPresent || !outerTransactionMatches || !merchantCodeMatches) {
                log.warn(
                        "Virtual trade callback rejected at payment fields: orderNo={}, productConfigured={}, envMatches={}, productMatches={}, quantityMatches={}, priceMatches={}, openidPresent={}, outerTransactionMatches={}, merchantCodeMatches={}",
                        orderNo,
                        productConfigured,
                        envMatches,
                        productMatches,
                        quantityMatches,
                        priceMatches,
                        openidPresent,
                        outerTransactionMatches,
                        merchantCodeMatches);
                throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_PAYMENT_NOT_CONFIGURED);
            }

            stage = "payer_binding";
            WeChatUserBindingEntity binding = bindingMapper.selectOne(new LambdaQueryWrapper<WeChatUserBindingEntity>()
                    .eq(WeChatUserBindingEntity::getTenantId, trade.getTenantId())
                    .eq(WeChatUserBindingEntity::getUserId, trade.getBuyerUserId())
                    .eq(WeChatUserBindingEntity::getAppId, pay.appId())
                    .eq(WeChatUserBindingEntity::getOpenid, openid)
                    .eq(WeChatUserBindingEntity::getStatus, "ACTIVE")
                    .eq(WeChatUserBindingEntity::getDeleted, 0)
                    .last("LIMIT 1"));
            if (binding == null) throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_PAYMENT_NOT_CONFIGURED);

            stage = "duplicate_transaction";
            if (StringUtils.hasText(transactionId)) {
                GoldBeanTradeEntity duplicate = tradeMapper.selectOne(
                        new LambdaQueryWrapper<GoldBeanTradeEntity>()
                                .eq(GoldBeanTradeEntity::getTransactionId, transactionId)
                                .ne(GoldBeanTradeEntity::getTradeNo, orderNo)
                                .eq(GoldBeanTradeEntity::getDeleted, 0)
                                .last("LIMIT 1"));
                if (duplicate != null) {
                    throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_PAYMENT_NOT_CONFIGURED);
                }
            }

            LocalDateTime now = LocalDateTime.now();
            trade.setTransactionId(transactionId);
            trade.setPaidAt(now);
            trade.setStatus(PAYMENT_CONFIRMED);
            trade.setUpdatedBy(trade.getBuyerUserId());
            trade.setUpdatedAt(now);
            tradeMapper.updateById(trade);
            stage = "seller_payout";
            initiateSellerPayout(trade);
        } catch (BusinessException exception) {
            log.warn(
                    "Virtual trade callback rejected: stage={}, orderNo={}, errorCode={}",
                    stage,
                    orderNo,
                    exception.getErrorCode().code());
            throw exception;
        } catch (Exception exception) {
            log.warn(
                    "Virtual trade callback failed: stage={}, orderNo={}, exceptionType={}",
                    stage,
                    orderNo,
                    exception.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_PAYMENT_NOT_CONFIGURED);
        }
    }

    private void handleWechatPaymentWithoutTenant(Transaction transaction) {
        if (transaction == null || !StringUtils.hasText(transaction.getOutTradeNo())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_NOT_FOUND);
        }
        GoldBeanTradeEntity trade = tradeMapper.selectOne(new LambdaQueryWrapper<GoldBeanTradeEntity>()
                .eq(GoldBeanTradeEntity::getTradeNo, transaction.getOutTradeNo())
                .eq(GoldBeanTradeEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (trade == null) throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_NOT_FOUND);
        if (COMPLETED.equals(trade.getStatus())) return;
        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        Integer amount = transaction.getAmount() == null ? null : transaction.getAmount().getTotal();
        if (!sellerTransferConfigured() || !pay.appId().equals(transaction.getAppid())
                || !pay.merchantId().equals(transaction.getMchid())
                || transaction.getTradeState() != Transaction.TradeStateEnum.SUCCESS
                || amount == null || amount.longValue() != paymentAmount(trade)
                || !StringUtils.hasText(transaction.getTransactionId())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_PAYMENT_NOT_CONFIGURED);
        }
        GoldBeanTradeEntity duplicate = tradeMapper.selectOne(new LambdaQueryWrapper<GoldBeanTradeEntity>()
                .eq(GoldBeanTradeEntity::getTransactionId, transaction.getTransactionId())
                .ne(GoldBeanTradeEntity::getTradeNo, trade.getTradeNo())
                .eq(GoldBeanTradeEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (duplicate != null) throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_PAYMENT_NOT_CONFIGURED);
        trade.setTransactionId(transaction.getTransactionId());
        trade.setPaidAt(LocalDateTime.now());
        trade.setStatus(PAYMENT_CONFIRMED);
        trade.setUpdatedAt(LocalDateTime.now());
        tradeMapper.updateById(trade);
        initiateSellerPayout(trade);
    }

    private void initiateSellerPayout(GoldBeanTradeEntity trade) {
        if (trade == null || COMPLETED.equals(trade.getStatus()) || REFUND_REQUIRED.equals(trade.getStatus())) {
            return;
        }
        if (!sellerTransferConfigured() || !StringUtils.hasText(transferSceneId)) {
            scheduleTradeRetry(trade, "商家转账能力未配置，配置恢复后自动重试");
            return;
        }
        WeChatUserBindingEntity binding = activeSellerBinding(trade);
        if (binding == null || !StringUtils.hasText(binding.getOpenid())) {
            scheduleTradeRetry(trade, "卖家尚未完成微信收款绑定，绑定后自动重试");
            return;
        }

        // New orders use one deterministic out_bill_no. Reusing it makes a timeout safe to retry.
        String outBillNo = StringUtils.hasText(trade.getTransferDetailNo())
                ? trade.getTransferDetailNo()
                : trade.getTradeNo() + "T";
        if (!StringUtils.hasText(trade.getTransferDetailNo())) {
            trade.setTransferDetailNo(outBillNo);
            trade.setStatus(TRANSFER_PENDING);
            trade.setTransferLastCheckedAt(LocalDateTime.now());
            trade.setTransferNextRetryAt(LocalDateTime.now().plusSeconds(settlementRetrySeconds));
            trade.setUpdatedAt(LocalDateTime.now());
            tradeMapper.updateById(trade);
        }

        // Orders created by the former batch-transfer implementation must never be paid twice.
        if (legacyTransferRecord(trade) || StringUtils.hasText(trade.getTransferBatchNo())) {
            syncTransfer(trade.getTradeNo());
            return;
        }
        try {
            applyTransferBill(trade, createSellerTransfer(trade, binding));
        } catch (RuntimeException exception) {
            scheduleTradeRetry(trade, "微信商家转账请求结果待查，系统将使用原单号恢复");
            log.warn(
                    "Gold-bean trade payout requires reconciliation: tradeNo={}, exceptionType={}",
                    trade.getTradeNo(),
                    exception.getClass().getSimpleName());
        }
    }

    private WeChatMerchantTransferClient.TransferBill createSellerTransfer(
            GoldBeanTradeEntity trade, WeChatUserBindingEntity binding) {
        String authorizationId = referralAuthorizationService.activeAuthorizationId(
                trade.getTenantId(), trade.getSellerUserId(), binding.getOpenid(), transferSceneId);
        return StringUtils.hasText(authorizationId)
                ? merchantTransferClient.transferAfterAuthorization(
                        trade.getTransferDetailNo(),
                        authorizationId,
                        trade.getTotalAmount(),
                        transferSceneId,
                        transferNotifyUrl,
                        transferUserRecvPerception,
                        transferJobType,
                        transferRewardDescription,
                        "三羊健康金豆交易结算")
                : merchantTransferClient.initiate(
                        trade.getTransferDetailNo(),
                        binding.getOpenid(),
                        trade.getTotalAmount(),
                        transferSceneId,
                        transferNotifyUrl,
                        transferUserRecvPerception,
                        transferJobType,
                        transferRewardDescription,
                        "三羊健康金豆交易结算");
    }

    @Transactional
    public void syncTransfer(String tradeNo) {
        GoldBeanTradeEntity trade = tradeMapper.selectOne(new LambdaQueryWrapper<GoldBeanTradeEntity>()
                .eq(GoldBeanTradeEntity::getTradeNo, tradeNo == null ? "" : tradeNo.trim())
                .eq(GoldBeanTradeEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (trade == null || COMPLETED.equals(trade.getStatus()) || REFUND_REQUIRED.equals(trade.getStatus())) return;
        if (!StringUtils.hasText(trade.getTransferDetailNo())) {
            if (PAYMENT_CONFIRMED.equals(trade.getStatus())) initiateSellerPayout(trade);
            return;
        }
        if (legacyTransferRecord(trade)) {
            syncLegacyTransfer(trade);
            return;
        }
        if (!sellerTransferConfigured() || !StringUtils.hasText(transferSceneId)) {
            scheduleTradeRetry(trade, "商家转账能力未配置，配置恢复后自动重试");
            return;
        }
        try {
            if (!StringUtils.hasText(trade.getTransferBatchNo())
                    && trade.getTransferLastCheckedAt() == null) {
                initiateSellerPayout(trade);
                return;
            }
            WeChatMerchantTransferClient.TransferBill bill = merchantTransferClient.query(
                    trade.getTransferDetailNo());
            if (bill == null
                    || (!StringUtils.hasText(bill.state())
                            && !StringUtils.hasText(bill.transferBillNo()))) {
                scheduleTradeRetry(trade, "微信转账单尚未返回明确状态");
                return;
            }
            applyTransferBill(trade, bill);
            if (confirmationPackageMissing(trade)) recoverTradeTransferPackage(trade);
        } catch (WeChatMerchantTransferClient.TransferBillNotFoundException exception) {
            // The create request may have timed out before WeChat persisted the bill. Retry the
            // same out_bill_no; it is the idempotency key and cannot create a second settlement.
            scheduleTradeRetry(trade, "微信转账单不存在，系统将使用原单号重新发起");
            initiateSellerPayout(trade);
        } catch (RuntimeException exception) {
            scheduleTradeRetry(trade, "微信转账单查询暂时失败，系统将稍后重试");
        }
    }

    private void syncLegacyTransfer(GoldBeanTradeEntity trade) {
        try {
            GetTransferDetailByOutNoRequest request = new GetTransferDetailByOutNoRequest();
            request.setOutBatchNo(trade.getTradeNo() + "B");
            request.setOutDetailNo(trade.getTransferDetailNo());
            TransferDetailEntity detail = weChatPayClient.transferBatch().getTransferDetailByOutNo(request);
            if (detail == null || !StringUtils.hasText(detail.getDetailStatus())) {
                scheduleTradeRetry(trade, "旧版微信转账单尚未返回明确状态");
                return;
            }
            String state = detail.getDetailStatus().trim().toUpperCase(Locale.ROOT);
            trade.setTransferWechatState(state);
            trade.setTransferLastCheckedAt(LocalDateTime.now());
            if ("SUCCESS".equals(state)) {
                trade.setFailureReason(null);
                trade.setTransferNextRetryAt(null);
                completeTrade(trade);
            } else if ("FAIL".equals(state) || "CLOSED".equals(state)) {
                trade.setStatus(REFUND_REQUIRED);
                trade.setFailureReason("卖家微信收款未成功，需平台处理");
                trade.setTransferNextRetryAt(null);
                trade.setUpdatedAt(LocalDateTime.now());
                tradeMapper.updateById(trade);
            } else {
                scheduleTradeRetry(trade, "旧版微信转账仍在处理中");
            }
        } catch (RuntimeException exception) {
            scheduleTradeRetry(trade, "旧版微信转账查询暂时失败，系统将稍后重试");
        }
    }

    /** Reconciles paid trades after a callback, process restart, or temporary WeChat failure. */
    @Transactional
    public void recoverPendingTrades() {
        if (!sellerTransferConfigured() || !StringUtils.hasText(transferSceneId)) return;
        LocalDateTime now = LocalDateTime.now();
        List<GoldBeanTradeEntity> candidates = TenantContext.executeReadWithoutTenant(
                () -> tradeMapper.selectList(new LambdaQueryWrapper<GoldBeanTradeEntity>()
                        .in(GoldBeanTradeEntity::getStatus, PAYMENT_CONFIRMED, TRANSFER_PENDING)
                        .eq(GoldBeanTradeEntity::getDeleted, 0)
                        .and(query -> query.isNull(GoldBeanTradeEntity::getTransferNextRetryAt)
                                .or()
                                .le(GoldBeanTradeEntity::getTransferNextRetryAt, now))
                        .orderByAsc(GoldBeanTradeEntity::getCreatedAt)
                        .last("LIMIT 50")));
        for (GoldBeanTradeEntity candidate : candidates) {
            try {
                TenantContext.execute(candidate.getTenantId(), () -> syncTransfer(candidate.getTradeNo()));
            } catch (RuntimeException exception) {
                log.warn(
                        "Gold-bean trade payout recovery failed: tradeNo={}, exceptionType={}",
                        candidate.getTradeNo(),
                        exception.getClass().getSimpleName());
            }
        }
    }

    private void applyTransferBill(
            GoldBeanTradeEntity trade, WeChatMerchantTransferClient.TransferBill bill) {
        if (bill == null) {
            scheduleTradeRetry(trade, "微信转账单尚未返回明确状态");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String state = bill.state() == null ? "" : bill.state().trim().toUpperCase(Locale.ROOT);
        if (StringUtils.hasText(bill.transferBillNo())) trade.setTransferBatchNo(bill.transferBillNo());
        if (StringUtils.hasText(state)) trade.setTransferWechatState(state);
        if (StringUtils.hasText(bill.packageInfo())) trade.setTransferPackageInfo(bill.packageInfo());
        trade.setTransferLastCheckedAt(now);
        if ("SUCCESS".equals(state)) {
            trade.setFailureReason(null);
            trade.setTransferNextRetryAt(null);
            completeTrade(trade);
        } else if ("FAIL".equals(state) || "CLOSED".equals(state) || "CANCELLED".equals(state)) {
            trade.setStatus(REFUND_REQUIRED);
            trade.setFailureReason(transferFailureReason(bill.failReason()));
            trade.setTransferNextRetryAt(null);
            trade.setUpdatedAt(now);
            tradeMapper.updateById(trade);
        } else {
            trade.setStatus(TRANSFER_PENDING);
            trade.setFailureReason(null);
            trade.setTransferNextRetryAt(now.plusSeconds(settlementRetrySeconds));
            trade.setUpdatedAt(now);
            tradeMapper.updateById(trade);
        }
    }

    private void recoverTradeTransferPackage(GoldBeanTradeEntity trade) {
        if (!confirmationPackageMissing(trade) || !StringUtils.hasText(trade.getTransferDetailNo())) return;
        WeChatUserBindingEntity binding = activeSellerBinding(trade);
        if (binding == null || !StringUtils.hasText(binding.getOpenid())) {
            scheduleTradeRetry(trade, "卖家尚未完成微信收款绑定，绑定后自动重试");
            return;
        }
        try {
            WeChatMerchantTransferClient.TransferBill response = createSellerTransfer(trade, binding);
            if (response == null
                    || (!StringUtils.hasText(response.state())
                            && !StringUtils.hasText(response.transferBillNo())
                            && !StringUtils.hasText(response.packageInfo()))) {
                scheduleTradeRetry(trade, "微信转账已受理，收款确认参数尚未返回，系统将继续恢复");
            } else {
                applyTransferBill(trade, response);
            }
        } catch (RuntimeException exception) {
            scheduleTradeRetry(trade, "微信收款确认参数恢复请求待重试");
        }
    }

    private boolean confirmationPackageMissing(GoldBeanTradeEntity trade) {
        String state = trade.getTransferWechatState();
        return ("WAIT_USER_CONFIRM".equals(state) || "TRANSFERING".equals(state))
                && !StringUtils.hasText(trade.getTransferPackageInfo());
    }

    private void scheduleTradeRetry(GoldBeanTradeEntity trade, String reason) {
        LocalDateTime now = LocalDateTime.now();
        if (!PAYMENT_CONFIRMED.equals(trade.getStatus()) && !TRANSFER_PENDING.equals(trade.getStatus())) {
            trade.setStatus(PAYMENT_CONFIRMED);
        }
        trade.setFailureReason(safeFailure(reason));
        trade.setTransferLastCheckedAt(now);
        trade.setTransferNextRetryAt(now.plusSeconds(settlementRetrySeconds));
        trade.setUpdatedAt(now);
        tradeMapper.updateById(trade);
    }

    private String transferFailureReason(String reason) {
        if (!StringUtils.hasText(reason)) return "卖家微信收款未成功，需平台处理";
        return safeFailure(reason);
    }

    private String safeFailure(String value) {
        String normalized = value == null ? "" : value.replaceAll("[\\r\\n\\t]", " ").trim();
        if (normalized.isBlank()) return "平台转账状态待确认";
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    private boolean legacyTransferRecord(GoldBeanTradeEntity trade) {
        return StringUtils.hasText(trade.getTransferBatchNo())
                && StringUtils.hasText(trade.getTransferDetailNo())
                && trade.getTransferDetailNo().endsWith("D");
    }

    private WeChatUserBindingEntity activeSellerBinding(GoldBeanTradeEntity trade) {
        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        return bindingMapper.selectOne(new LambdaQueryWrapper<WeChatUserBindingEntity>()
                .eq(WeChatUserBindingEntity::getTenantId, trade.getTenantId())
                .eq(WeChatUserBindingEntity::getUserId, trade.getSellerUserId())
                .eq(WeChatUserBindingEntity::getAppId, pay.appId())
                .eq(WeChatUserBindingEntity::getStatus, "ACTIVE")
                .eq(WeChatUserBindingEntity::getDeleted, 0)
                .last("LIMIT 1"));
    }

    void completeTrade(GoldBeanTradeEntity trade) {
        if (COMPLETED.equals(trade.getStatus())) return;
        GoldBeanAccountEntity buyer;
        GoldBeanAccountEntity seller;
        if (trade.getBuyerUserId() < trade.getSellerUserId()) {
            buyer = lockedAccount(trade.getTenantId(), trade.getBuyerUserId());
            seller = lockedAccount(trade.getTenantId(), trade.getSellerUserId());
        } else {
            seller = lockedAccount(trade.getTenantId(), trade.getSellerUserId());
            buyer = lockedAccount(trade.getTenantId(), trade.getBuyerUserId());
        }
        requireRegistered(buyer);
        requireRegistered(seller);
        goldBeanService.settleForTrade(buyer, buyer.getUserId());
        goldBeanService.settleForTrade(seller, seller.getUserId());
        String bucket = normalizeBucket(trade.getBucket());
        String bucketName = DIGITAL_BANK.equals(bucket) ? "数字银行" : "可交易";
        goldBeanService.debitForTrade(seller, bucket, trade.getQuantity(), "TRADE_SETTLEMENT_SELL",
                buyer.getUserId(), trade.getTradeNo() + ":SELLER:" + bucket,
                "已完成微信收款并交割" + bucketName + "金豆");
        String buyerCreditMode = normalizeBuyerCreditMode(trade.getBuyerCreditMode());
        if (BUYER_CREDIT_DIGITAL_BANK.equals(buyerCreditMode)) {
            goldBeanService.creditForTradeDigitalBank(buyer, trade.getQuantity(), "TRADE_SETTLEMENT_BUY",
                    seller.getUserId(), trade.getTradeNo() + ":BUYER:DIGITAL_BANK",
                    "已完成微信付款并获得数字银行金豆");
        } else {
            goldBeanService.creditForTradeSplit(buyer, trade.getQuantity(), "TRADE_SETTLEMENT_BUY",
                    seller.getUserId(), trade.getTradeNo() + ":BUYER:SPLIT",
                    "已完成微信付款并获得金豆（一半进入可交易金豆、一半进入数字银行）");
        }
        trade.setStatus(COMPLETED);
        trade.setTransferredAt(LocalDateTime.now());
        trade.setUpdatedAt(LocalDateTime.now());
        tradeMapper.updateById(trade);
    }

    private void closeIfExpired(GoldBeanTradeEntity trade) {
        if (!PENDING_PAYMENT.equals(trade.getStatus()) || trade.getExpiresAt() == null
                || LocalDateTime.now().isBefore(trade.getExpiresAt())) return;
        trade.setStatus(CANCELLED);
        trade.setUpdatedAt(LocalDateTime.now());
        tradeMapper.updateById(trade);
        releaseListing(trade, trade.getBuyerUserId());
    }

    private void releaseListing(GoldBeanTradeEntity trade, long operatorId) {
        GoldBeanTradeListingEntity listing = listingMapper.selectOne(new LambdaQueryWrapper<GoldBeanTradeListingEntity>()
                .eq(GoldBeanTradeListingEntity::getId, trade.getListingId())
                .eq(GoldBeanTradeListingEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (listing == null || !CANCELLED.equals(trade.getStatus())) return;
        listing.setRemainingQuantity(GoldBeanAmounts.normalize(listing.getRemainingQuantity().add(trade.getQuantity())));
        if (GoldBeanAmounts.positive(listing.getRemainingQuantity())) listing.setStatus(OPEN);
        listing.setUpdatedBy(operatorId);
        listing.setUpdatedAt(LocalDateTime.now());
        listingMapper.updateById(listing);
    }

    private boolean sellerTransferConfigured() {
        return paymentEnabled && goldBeanService.available()
                && membershipProperties.wechatPay().configured() && weChatPayClient.configured();
    }

    private boolean tradePaymentConfigured() {
        return sellerTransferConfigured()
                && StringUtils.hasText(transferSceneId)
                && goldBeanProperties.paymentEnabled()
                && virtualPayClient.configuredFor(goldBeanProperties.goldBeanProductId());
    }

    private CurrentPrincipal requireCustomer() {
        if (!goldBeanService.available()) throw new BusinessException(ErrorCode.GOLD_BEAN_NOT_ENABLED);
        CurrentPrincipal current = CurrentUser.require();
        if (!"CUSTOMER".equals(current.workbench())) throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        return current;
    }

    private GoldBeanTradeListingEntity lockedListing(String value, long tenantId) {
        GoldBeanTradeListingEntity listing = listingMapper.selectOne(new LambdaQueryWrapper<GoldBeanTradeListingEntity>()
                .eq(GoldBeanTradeListingEntity::getId, parseId(value))
                .eq(GoldBeanTradeListingEntity::getTenantId, tenantId)
                .eq(GoldBeanTradeListingEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (listing == null) throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_NOT_FOUND);
        return listing;
    }

    private GoldBeanTradeEntity ownedTrade(String value, CurrentPrincipal current, boolean lock) {
        LambdaQueryWrapper<GoldBeanTradeEntity> query = new LambdaQueryWrapper<GoldBeanTradeEntity>()
                .eq(GoldBeanTradeEntity::getTradeNo, value == null ? "" : value.trim())
                .eq(GoldBeanTradeEntity::getTenantId, current.tenantId())
                .eq(GoldBeanTradeEntity::getBuyerUserId, current.userId())
                .eq(GoldBeanTradeEntity::getDeleted, 0);
        query.last(lock ? "LIMIT 1 FOR UPDATE" : "LIMIT 1");
        GoldBeanTradeEntity trade = tradeMapper.selectOne(query);
        if (trade == null) throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_NOT_FOUND);
        return trade;
    }

    private GoldBeanAccountEntity lockedAccount(long tenantId, long userId) {
        GoldBeanAccountEntity account = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, tenantId)
                .eq(GoldBeanAccountEntity::getUserId, userId)
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
        if (account == null) throw new BusinessException(ErrorCode.GOLD_BEAN_ACCOUNT_NOT_FOUND);
        return account;
    }

    private GoldBeanAccountEntity currentAccount(CurrentPrincipal current) {
        GoldBeanAccountEntity account = accountMapper.selectOne(new LambdaQueryWrapper<GoldBeanAccountEntity>()
                .eq(GoldBeanAccountEntity::getTenantId, current.tenantId())
                .eq(GoldBeanAccountEntity::getUserId, current.userId())
                .eq(GoldBeanAccountEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (account == null) throw new BusinessException(ErrorCode.GOLD_BEAN_ACCOUNT_NOT_FOUND);
        return account;
    }

    private void requireRegistered(GoldBeanAccountEntity account) {
        if (!PAID.equals(account.getRegistrationFeeStatus()) || !"ACTIVE".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PAYMENT_REQUIRED);
        }
    }

    private String requireTradeRegion(GoldBeanAccountEntity account) {
        String regionCity = normalizeTradeRegion(account == null ? null : account.getCity());
        if (regionCity.isBlank()) throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_REGION_REQUIRED);
        return regionCity;
    }

    private boolean sameTradeRegion(String buyerRegionCity, String listingRegionCity) {
        String normalizedListingRegion = normalizeTradeRegion(listingRegionCity);
        return !normalizedListingRegion.isBlank() && buyerRegionCity.equals(normalizedListingRegion);
    }

    private String normalizeTradeRegion(String value) {
        if (value == null) return "";
        return TextEncodingUtils.repairUtf8Mojibake(value.trim()).replaceAll("\\s+", " ");
    }

    private BigDecimal openListings(long tenantId, long sellerId) {
        return openListings(tenantId, sellerId, null);
    }

    private BigDecimal openListings(long tenantId, long sellerId, String bucket) {
        LambdaQueryWrapper<GoldBeanTradeListingEntity> query = new LambdaQueryWrapper<GoldBeanTradeListingEntity>()
                .eq(GoldBeanTradeListingEntity::getTenantId, tenantId)
                .eq(GoldBeanTradeListingEntity::getSellerUserId, sellerId)
                .eq(GoldBeanTradeListingEntity::getStatus, OPEN)
                .eq(GoldBeanTradeListingEntity::getDeleted, 0);
        if (bucket != null) query.eq(GoldBeanTradeListingEntity::getBucket, bucket);
        return listingMapper.selectList(query).stream()
                .map(GoldBeanTradeListingEntity::getRemainingQuantity)
                .map(GoldBeanAmounts::nonNegative)
                .reduce(GoldBeanAmounts.ZERO, BigDecimal::add);
    }

    private BigDecimal pendingTradeQuantity(long tenantId, long sellerId) {
        return pendingTradeQuantity(tenantId, sellerId, null);
    }

    private BigDecimal pendingTradeQuantity(long tenantId, long sellerId, String bucket) {
        LambdaQueryWrapper<GoldBeanTradeEntity> query = new LambdaQueryWrapper<GoldBeanTradeEntity>()
                .eq(GoldBeanTradeEntity::getTenantId, tenantId)
                .eq(GoldBeanTradeEntity::getSellerUserId, sellerId)
                .in(GoldBeanTradeEntity::getStatus, PENDING_PAYMENT, PAYMENT_CONFIRMED, TRANSFER_PENDING)
                .eq(GoldBeanTradeEntity::getDeleted, 0);
        if (bucket != null) query.eq(GoldBeanTradeEntity::getBucket, bucket);
        return tradeMapper.selectList(query).stream()
                .map(GoldBeanTradeEntity::getQuantity)
                .map(GoldBeanAmounts::nonNegative)
                .reduce(GoldBeanAmounts.ZERO, BigDecimal::add);
    }

    private long normalizePage(long value) {
        return Math.max(1L, value);
    }

    private long normalizePageSize(long value) {
        return Math.min(50L, Math.max(1L, value));
    }

    private BigDecimal saturatingAdd(BigDecimal left, BigDecimal right) {
        return GoldBeanAmounts.nonNegative(left).add(GoldBeanAmounts.nonNegative(right));
    }

    private BigDecimal percentageOf(BigDecimal balance, int percent) {
        return GoldBeanAmounts.percentage(balance, percent);
    }

    private BigDecimal safeBalance(BigDecimal value) {
        return GoldBeanAmounts.nonNegative(value);
    }

    private String normalizeBucket(String value) {
        if (value == null || value.isBlank()) return TRADING;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (TRADING.equals(normalized) || DIGITAL_BANK.equals(normalized)) return normalized;
        throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_BUCKET_INVALID);
    }

    private String normalizeBuyerCreditMode(String value) {
        if (value == null || value.isBlank()) return BUYER_CREDIT_SPLIT;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (BUYER_CREDIT_SPLIT.equals(normalized) || BUYER_CREDIT_DIGITAL_BANK.equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_AMOUNT_INVALID);
    }

    private String normalizeTransferText(String value, String fallback) {
        if (!StringUtils.hasText(value)) return fallback;
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    private long parseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_LISTING_NOT_FOUND);
        }
    }

    private GoldBeanTradeListingVo toListing(GoldBeanTradeListingEntity item, long userId) {
        long total = GoldBeanAmounts.currencyCents(item.getRemainingQuantity(), FIXED_UNIT_PRICE_CENT);
        return new GoldBeanTradeListingVo(String.valueOf(item.getId()), normalizeBucket(item.getBucket()),
                normalizeTradeRegion(item.getRegionCity()), item.getQuantity(), item.getRemainingQuantity(),
                FIXED_UNIT_PRICE_CENT, total, item.getStatus(),
                item.getSellerUserId().equals(userId), item.getCreatedAt());
    }

    private GoldBeanTradeVo toTrade(GoldBeanTradeEntity item) {
        return new GoldBeanTradeVo(item.getTradeNo(), String.valueOf(item.getListingId()), normalizeBucket(item.getBucket()), item.getQuantity(),
                item.getUnitPriceCent(), item.getTotalAmount(), paymentAmount(item), item.getStatus(), tradePaymentConfigured(), item.getCreatedAt());
    }

    /** Fractional bean trades use one virtual good priced at the complete payable amount. */
    private int paymentGoodsQuantity(GoldBeanTradeEntity trade) {
        BigDecimal quantity = GoldBeanAmounts.normalize(trade.getQuantity());
        try {
            return quantity.stripTrailingZeros().scale() <= 0
                    ? quantity.intValueExact()
                    : 1;
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_TRADE_AMOUNT_INVALID);
        }
    }

    private int paymentGoodsPrice(GoldBeanTradeEntity trade) {
        BigDecimal quantity = GoldBeanAmounts.normalize(trade.getQuantity());
        if (quantity.stripTrailingZeros().scale() > 0) {
            return Math.toIntExact(paymentAmount(trade));
        }
        return Math.toIntExact(paymentUnitPriceCent(trade));
    }

    /** Historical trades have no surcharge snapshot and continue to use their original amount. */
    private long paymentUnitPriceCent(GoldBeanTradeEntity trade) {
        return trade.getPaymentUnitPriceCent() == null
                ? trade.getUnitPriceCent()
                : trade.getPaymentUnitPriceCent();
    }

    private long paymentAmount(GoldBeanTradeEntity trade) {
        return trade.getPaymentAmount() == null
                ? trade.getTotalAmount()
                : trade.getPaymentAmount();
    }

    private GoldBeanTradePayoutVo toPayout(GoldBeanTradeEntity trade) {
        String state = trade.getTransferWechatState();
        boolean confirmation = ("WAIT_USER_CONFIRM".equals(state) || "TRANSFERING".equals(state))
                && StringUtils.hasText(trade.getTransferPackageInfo());
        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        return new GoldBeanTradePayoutVo(
                trade.getTradeNo(),
                GoldBeanAmounts.nonNegative(trade.getQuantity()),
                trade.getTotalAmount() == null ? 0L : trade.getTotalAmount(),
                normalizeBucket(trade.getBucket()),
                trade.getStatus(),
                state,
                confirmation ? pay.merchantId() : null,
                confirmation ? pay.appId() : null,
                confirmation,
                confirmation ? trade.getTransferPackageInfo() : null,
                trade.getCreatedAt(),
                trade.getTransferredAt(),
                trade.getFailureReason());
    }

    private boolean isSupportedVirtualPaymentEvent(String event, String eventType) {
        if ("xpay_goods_deliver_notify".equalsIgnoreCase(event)) {
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

    private void audit(Object entity, long userId, LocalDateTime now) {
        if (entity instanceof GoldBeanTradeListingEntity listing) {
            listing.setCreatedBy(userId); listing.setCreatedAt(now); listing.setUpdatedBy(userId);
            listing.setUpdatedAt(now); listing.setDeleted(0); listing.setVersion(0);
        } else if (entity instanceof GoldBeanTradeEntity trade) {
            trade.setCreatedBy(userId); trade.setCreatedAt(now); trade.setUpdatedBy(userId);
            trade.setUpdatedAt(now); trade.setDeleted(0); trade.setVersion(0);
        }
    }
}
