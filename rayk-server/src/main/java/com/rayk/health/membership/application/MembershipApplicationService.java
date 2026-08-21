package com.rayk.health.membership.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.membership.config.MembershipProperties;
import com.rayk.health.membership.dto.CreateMembershipOrderRequest;
import com.rayk.health.membership.dto.PayMembershipOrderRequest;
import com.rayk.health.membership.entity.CustomerMembershipEntity;
import com.rayk.health.membership.entity.MembershipBenefitEntity;
import com.rayk.health.membership.entity.MembershipOrderEntity;
import com.rayk.health.membership.entity.MembershipPlanBenefitEntity;
import com.rayk.health.membership.entity.MembershipPlanEntity;
import com.rayk.health.membership.entity.MembershipUsageEntity;
import com.rayk.health.membership.mapper.MembershipBenefitMapper;
import com.rayk.health.membership.mapper.CustomerMembershipMapper;
import com.rayk.health.membership.mapper.MembershipOrderMapper;
import com.rayk.health.membership.mapper.MembershipPlanBenefitMapper;
import com.rayk.health.membership.mapper.MembershipPlanMapper;
import com.rayk.health.membership.mapper.MembershipUsageMapper;
import com.rayk.health.membership.payment.WeChatVirtualPayClient;
import com.rayk.health.membership.vo.MembershipBenefitVo;
import com.rayk.health.membership.vo.MembershipOrderVo;
import com.rayk.health.membership.vo.MembershipPaymentVo;
import com.rayk.health.membership.vo.MembershipPlanVo;
import com.rayk.health.membership.vo.MembershipSummaryVo;
import com.rayk.health.membership.vo.MembershipUsageVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.wechat.entity.WeChatUserBindingEntity;
import com.rayk.health.security.wechat.mapper.WeChatUserBindingMapper;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MembershipApplicationService {
    private final MembershipProperties properties;
    private final MembershipEntitlementService entitlementService;
    private final MembershipPlanMapper planMapper;
    private final MembershipPlanBenefitMapper planBenefitMapper;
    private final MembershipBenefitMapper benefitMapper;
    private final MembershipUsageMapper usageMapper;
    private final MembershipOrderMapper orderMapper;
    private final CustomerMembershipMapper customerMembershipMapper;
    private final WeChatUserBindingMapper bindingMapper;
    private final WeChatVirtualPayClient weChatVirtualPayClient;
    private final ObjectMapper objectMapper;

    public MembershipApplicationService(
            MembershipProperties properties,
            MembershipEntitlementService entitlementService,
            MembershipPlanMapper planMapper,
            MembershipPlanBenefitMapper planBenefitMapper,
            MembershipBenefitMapper benefitMapper,
            MembershipUsageMapper usageMapper,
            MembershipOrderMapper orderMapper,
            CustomerMembershipMapper customerMembershipMapper,
            WeChatUserBindingMapper bindingMapper,
            WeChatVirtualPayClient weChatVirtualPayClient,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.entitlementService = entitlementService;
        this.planMapper = planMapper;
        this.planBenefitMapper = planBenefitMapper;
        this.benefitMapper = benefitMapper;
        this.usageMapper = usageMapper;
        this.orderMapper = orderMapper;
        this.customerMembershipMapper = customerMembershipMapper;
        this.bindingMapper = bindingMapper;
        this.weChatVirtualPayClient = weChatVirtualPayClient;
        this.objectMapper = objectMapper;
    }

    public MembershipSummaryVo summary() {
        CurrentPrincipal current = CurrentUser.require();
        MembershipEntitlementService.MembershipSnapshot snapshot = entitlementService.snapshot(current);
        if (snapshot.plan() == null) {
            return new MembershipSummaryVo("DISABLED", null, null, null, null, 0, false,
                    properties.paymentEnabled(), List.of());
        }
        return new MembershipSummaryVo(
                snapshot.paidActive() ? "ACTIVE" : "FREE",
                snapshot.plan().getPlanCode(),
                snapshot.plan().getPlanName(),
                snapshot.membership().getStartAt(),
                snapshot.membership().getExpireAt(),
                remainingDays(snapshot.membership().getExpireAt(), snapshot.paidActive()),
                snapshot.paidActive(),
                properties.paymentEnabled(),
                benefits(snapshot));
    }

    /**
     * Switches the current customer's membership for local/WeChat development builds only.
     * Existing records are retained for audit; paid memberships are marked expired before a
     * new state is selected. The production server rejects this operation by configuration.
     */
    @Transactional
    public MembershipSummaryVo switchForDevelopment(String target) {
        if (!properties.developmentMode()) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        CurrentPrincipal current = CurrentUser.require();
        String normalized = target == null ? "" : target.trim().toUpperCase(java.util.Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        expireActivePaidMemberships(current, now);
        if ("FREE".equals(normalized)) {
            // summary() will create the permanent free membership when this customer does not
            // have one yet, and will return the free plan after the paid record is expired.
            return summary();
        }
        if (!"YEARLY".equals(normalized)) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        MembershipPlanEntity plan = entitlementService.plan("AI_HEALTH_YEARLY");
        if (plan == null) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        CustomerMembershipEntity membership = new CustomerMembershipEntity();
        membership.setTenantId(current.tenantId());
        membership.setCustomerId(current.userId());
        membership.setPlanId(plan.getId());
        membership.setStatus("ACTIVE");
        membership.setStartAt(now);
        membership.setExpireAt(now.plusDays(plan.getDurationDays()));
        membership.setSource("DEVELOPMENT_SWITCH");
        membership.setCreatedBy(current.userId());
        membership.setCreatedAt(now);
        membership.setUpdatedBy(current.userId());
        membership.setUpdatedAt(now);
        membership.setDeleted(0);
        membership.setVersion(0);
        customerMembershipMapper.insert(membership);
        return summary();
    }

    private void expireActivePaidMemberships(CurrentPrincipal current, LocalDateTime now) {
        List<CustomerMembershipEntity> memberships = customerMembershipMapper.selectList(
                new LambdaQueryWrapper<CustomerMembershipEntity>()
                        .eq(CustomerMembershipEntity::getTenantId, current.tenantId())
                        .eq(CustomerMembershipEntity::getCustomerId, current.userId())
                        .eq(CustomerMembershipEntity::getStatus, "ACTIVE")
                        .eq(CustomerMembershipEntity::getDeleted, 0));
        for (CustomerMembershipEntity membership : memberships) {
            MembershipPlanEntity plan = planMapper.selectById(membership.getPlanId());
            if (plan != null && !MembershipEntitlementService.FREE_PLAN_CODE.equals(plan.getPlanCode())) {
                membership.setStatus("EXPIRED");
                membership.setExpireAt(now);
                membership.setUpdatedBy(current.userId());
                membership.setUpdatedAt(now);
                customerMembershipMapper.updateById(membership);
            }
        }
    }

    public List<MembershipPlanVo> plans() {
        Map<Long, List<String>> benefitNames = planBenefitMapper.selectList(new LambdaQueryWrapper<MembershipPlanBenefitEntity>()
                        .eq(MembershipPlanBenefitEntity::getEnabled, true)
                        .eq(MembershipPlanBenefitEntity::getDeleted, 0))
                .stream()
                .collect(Collectors.groupingBy(MembershipPlanBenefitEntity::getPlanId,
                        Collectors.mapping(MembershipPlanBenefitEntity::getBenefitCode, Collectors.toList())));
        Map<String, MembershipBenefitEntity> benefits = benefitMapper.selectList(new LambdaQueryWrapper<MembershipBenefitEntity>()
                        .eq(MembershipBenefitEntity::getStatus, "ACTIVE")
                        .eq(MembershipBenefitEntity::getDeleted, 0))
                .stream().collect(Collectors.toMap(MembershipBenefitEntity::getBenefitCode, Function.identity()));
        return planMapper.selectList(new LambdaQueryWrapper<MembershipPlanEntity>()
                        .eq(MembershipPlanEntity::getStatus, "ACTIVE")
                        .eq(MembershipPlanEntity::getDeleted, 0)
                        .orderByAsc(MembershipPlanEntity::getSortOrder))
                .stream()
                .map(plan -> new MembershipPlanVo(plan.getPlanCode(), plan.getPlanName(), plan.getDurationDays(),
                        plan.getPriceCent(), plan.getOriginalPriceCent(), plan.getDescription(),
                        benefitNames.getOrDefault(plan.getId(), List.of()).stream()
                                .map(code -> benefits.containsKey(code) ? benefits.get(code).getBenefitName() : code).toList()))
                .toList();
    }

    public List<MembershipBenefitVo> benefits() {
        MembershipEntitlementService.MembershipSnapshot snapshot = entitlementService.snapshot(CurrentUser.require());
        return benefits(snapshot);
    }

    public List<MembershipUsageVo> usage() {
        CurrentPrincipal current = CurrentUser.require();
        Map<String, String> names = benefitMapper.selectList(new LambdaQueryWrapper<MembershipBenefitEntity>()
                        .eq(MembershipBenefitEntity::getDeleted, 0)).stream()
                .collect(Collectors.toMap(MembershipBenefitEntity::getBenefitCode, MembershipBenefitEntity::getBenefitName));
        return usageMapper.selectList(new LambdaQueryWrapper<MembershipUsageEntity>()
                        .eq(MembershipUsageEntity::getTenantId, current.tenantId())
                        .eq(MembershipUsageEntity::getCustomerId, current.userId())
                        .eq(MembershipUsageEntity::getDeleted, 0)
                        .orderByDesc(MembershipUsageEntity::getCreatedAt)
                        .last("LIMIT 100"))
                .stream().map(item -> new MembershipUsageVo(item.getBenefitCode(), names.get(item.getBenefitCode()),
                        item.getUsageStatus(), item.getBizType(), item.getBizId(),
                        Objects.requireNonNullElse(item.getConfirmedAt(), item.getReservedAt())))
                .toList();
    }

    @Transactional
    public MembershipOrderVo createOrder(@Valid CreateMembershipOrderRequest request) {
        CurrentPrincipal current = CurrentUser.require();
        MembershipPlanEntity plan = entitlementService.plan(request.planCode());
        if (plan == null || !"AI_HEALTH_YEARLY".equals(plan.getPlanCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        LocalDateTime now = LocalDateTime.now();
        MembershipOrderEntity order = new MembershipOrderEntity();
        order.setTenantId(current.tenantId());
        order.setCustomerId(current.userId());
        order.setPlanId(plan.getId());
        order.setOrderNo("M" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        order.setStatus("PENDING");
        order.setAmountCent(plan.getPriceCent());
        order.setCreatedBy(current.userId());
        order.setCreatedAt(now);
        order.setUpdatedBy(current.userId());
        order.setUpdatedAt(now);
        order.setDeleted(0);
        order.setVersion(0);
        orderMapper.insert(order);
        return toOrderVo(order, plan, false);
    }

    public MembershipOrderVo order(String orderNo) {
        MembershipOrderEntity order = ownedOrder(orderNo);
        MembershipPlanEntity plan = planMapper.selectById(order.getPlanId());
        return toOrderVo(order, plan, "SIMULATED".equals(order.getPaymentChannel()));
    }

    /** Creates the signed parameters used by wx.requestVirtualPayment. */
    public MembershipPaymentVo createWechatPayment(String orderNo) {
        if (!properties.paymentEnabled()) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_NOT_CONFIGURED);
        }
        if (!weChatVirtualPayClient.configured()) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_NOT_CONFIGURED);
        }
        CurrentPrincipal current = CurrentUser.require();
        MembershipOrderEntity order = ownedOrder(orderNo);
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_BENEFIT_NOT_AVAILABLE);
        }
        return weChatVirtualPayClient.createGoodsPayment(
                order.getOrderNo(), order.getAmountCent(), current.userId());
    }

    /** Handles a verified-by-content virtual-payment goods delivery notification. */
    @Transactional
    public void handleVirtualPaymentNotification(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!"xpay_goods_deliver_notify".equals(text(root, "Event", "event"))) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_UNAVAILABLE);
            }
            String orderNo = text(root, "OutTradeNo", "out_trade_no");
            if (orderNo == null) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_ORDER_NOT_FOUND);
            }
            MembershipOrderEntity order = orderMapper.selectOne(
                    new LambdaQueryWrapper<MembershipOrderEntity>()
                            .eq(MembershipOrderEntity::getOrderNo, orderNo)
                            .eq(MembershipOrderEntity::getDeleted, 0)
                            .last("LIMIT 1 FOR UPDATE"));
            if (order == null) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_ORDER_NOT_FOUND);
            }
            if ("PAID".equals(order.getStatus())) {
                return;
            }

            MembershipProperties.WeChatVirtualPayProperties pay = properties.wechatVirtualPay();
            int env = integer(root, "Env", "env");
            String openid = text(root, "OpenId", "openid");
            JsonNode goods = child(root, "GoodsInfo", "goodsInfo");
            JsonNode payment = child(root, "WeChatPayInfo", "weChatPayInfo");
            String productId = text(goods, "ProductId", "productId");
            int quantity = integer(goods, "Quantity", "quantity");
            int actualPrice = integer(goods, "ActualPrice", "actualPrice");
            String transactionId = text(payment, "TransactionId", "transactionId");
            String merchantOrderNo = text(payment, "MchOrderNo", "mchOrderNo");
            String merchantCode = text(root, "MerchantCode", "merchantCode");

            if (!pay.configured()
                    || env != pay.env()
                    || !pay.productId().equals(productId)
                    || quantity != 1
                    || actualPrice != order.getAmountCent()
                    || !StringUtils.hasText(openid)
                    || !StringUtils.hasText(transactionId)
                    || merchantOrderNo != null && !orderNo.equals(merchantOrderNo)
                    || merchantCode != null && !pay.merchantId().equals(merchantCode)) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_UNAVAILABLE);
            }
            WeChatUserBindingEntity binding = bindingMapper.selectOne(
                    new LambdaQueryWrapper<WeChatUserBindingEntity>()
                            .eq(WeChatUserBindingEntity::getTenantId, order.getTenantId())
                            .eq(WeChatUserBindingEntity::getUserId, order.getCustomerId())
                            .eq(WeChatUserBindingEntity::getAppId, pay.appId())
                            .eq(WeChatUserBindingEntity::getOpenid, openid)
                            .eq(WeChatUserBindingEntity::getDeleted, 0)
                            .last("LIMIT 1"));
            if (binding == null) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_UNAVAILABLE);
            }
            MembershipOrderEntity duplicateTransaction = orderMapper.selectOne(
                    new LambdaQueryWrapper<MembershipOrderEntity>()
                            .eq(MembershipOrderEntity::getTransactionId, transactionId)
                            .ne(MembershipOrderEntity::getOrderNo, orderNo)
                            .eq(MembershipOrderEntity::getDeleted, 0)
                            .last("LIMIT 1"));
            if (duplicateTransaction != null) {
                throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_UNAVAILABLE);
            }
            MembershipPlanEntity plan = planMapper.selectById(order.getPlanId());
            if (plan == null) {
                throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
            }
            LocalDateTime now = LocalDateTime.now();
            order.setStatus("PAID");
            order.setPaymentChannel("WECHAT_VIRTUAL");
            order.setTransactionId(transactionId);
            order.setPaidAt(now);
            order.setPaymentNotifyAt(now);
            order.setUpdatedBy(order.getCustomerId());
            order.setUpdatedAt(now);
            orderMapper.updateById(order);
            activateMembership(order, plan, now, "WECHAT_VIRTUAL_ORDER");
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_UNAVAILABLE);
        }
    }

    /** Handles a verified WeChat Pay notification and activates the membership exactly once. */
    @Transactional
    public void handleWechatPayment(Transaction transaction) {
        if (transaction == null || transaction.getOutTradeNo() == null) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_ORDER_NOT_FOUND);
        }
        MembershipOrderEntity order = orderMapper.selectOne(
                new LambdaQueryWrapper<MembershipOrderEntity>()
                        .eq(MembershipOrderEntity::getOrderNo, transaction.getOutTradeNo())
                        .eq(MembershipOrderEntity::getDeleted, 0)
                        .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_ORDER_NOT_FOUND);
        }
        if ("PAID".equals(order.getStatus())) {
            return;
        }
        MembershipProperties.WeChatPayProperties pay = properties.wechatPay();
        Integer paidAmount = transaction.getAmount() == null ? null : transaction.getAmount().getTotal();
        if (!properties.paymentEnabled()
                || !pay.appId().equals(transaction.getAppid())
                || !pay.merchantId().equals(transaction.getMchid())
                || transaction.getTradeState() != Transaction.TradeStateEnum.SUCCESS
                || paidAmount == null
                || paidAmount.intValue() != order.getAmountCent()
                || transaction.getTransactionId() == null
                || transaction.getTransactionId().isBlank()) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_UNAVAILABLE);
        }
        MembershipPlanEntity plan = planMapper.selectById(order.getPlanId());
        if (plan == null) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus("PAID");
        order.setPaymentChannel("WECHAT_JSAPI");
        order.setTransactionId(transaction.getTransactionId());
        order.setPaidAt(now);
        order.setPaymentNotifyAt(now);
        order.setUpdatedBy(order.getCustomerId());
        order.setUpdatedAt(now);
        orderMapper.updateById(order);
        activateMembership(order, plan, now, "WECHAT_ORDER");
    }

    private String text(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull() && StringUtils.hasText(value.asText())) {
                return value.asText().trim();
            }
        }
        return null;
    }

    private JsonNode child(JsonNode node, String... names) {
        if (node == null) {
            return null;
        }
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                return value;
            }
        }
        return null;
    }

    private int integer(JsonNode node, String... names) {
        if (node == null) {
            return -1;
        }
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isIntegralNumber()) {
                return value.asInt(-1);
            }
            try {
                return Integer.parseInt(value.asText());
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    @Transactional
    public MembershipOrderVo pay(String orderNo, PayMembershipOrderRequest request) {
        CurrentPrincipal current = CurrentUser.require();
        MembershipOrderEntity order = ownedOrder(orderNo);
        MembershipPlanEntity plan = planMapper.selectById(order.getPlanId());
        if (!"PENDING".equals(order.getStatus())) return toOrderVo(order, plan, "SIMULATED".equals(order.getPaymentChannel()));
        if (properties.paymentEnabled()) {
            throw new BusinessException(ErrorCode.MEMBERSHIP_PAYMENT_NOT_CONFIGURED);
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus("PAID");
        order.setPaymentChannel("SIMULATED");
        order.setPaidAt(now);
        order.setUpdatedBy(current.userId());
        order.setUpdatedAt(now);
        orderMapper.updateById(order);

        activateMembership(order, plan, now, "SIMULATED_ORDER");
        return toOrderVo(order, plan, true);
    }

    private List<MembershipBenefitVo> benefits(MembershipEntitlementService.MembershipSnapshot snapshot) {
        if (snapshot.plan() == null) return List.of();
        Map<String, MembershipBenefitEntity> all = benefitMapper.selectList(new LambdaQueryWrapper<MembershipBenefitEntity>()
                        .eq(MembershipBenefitEntity::getStatus, "ACTIVE")
                        .eq(MembershipBenefitEntity::getDeleted, 0)).stream()
                .collect(Collectors.toMap(MembershipBenefitEntity::getBenefitCode, Function.identity()));
        return snapshot.planBenefits().stream().map(config -> {
            MembershipBenefitEntity benefit = all.get(config.getBenefitCode());
            if (benefit == null) return null;
            int used = entitlementService.usageCount(snapshot, config.getBenefitCode(), "SUMMARY", null);
            Integer quota = entitlementService.effectiveQuota(snapshot, config);
            Integer remaining = quota == null ? null : Math.max(0, quota - used);
            boolean available = "ENABLED".equals(benefit.getUnitType()) ? snapshot.paidActive() : remaining == null || remaining > 0;
            return new MembershipBenefitVo(config.getBenefitCode(), benefit.getBenefitName(), benefit.getDescription(),
                    benefit.getUnitType(), quota, used, remaining, available);
        }).filter(Objects::nonNull).toList();
    }

    private long remainingDays(LocalDateTime expireAt, boolean paid) {
        return paid ? Math.max(0, Duration.between(LocalDateTime.now(), expireAt).toDays()) : 0;
    }

    private MembershipOrderEntity ownedOrder(String orderNo) {
        CurrentPrincipal current = CurrentUser.require();
        MembershipOrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<MembershipOrderEntity>()
                .eq(MembershipOrderEntity::getTenantId, current.tenantId())
                .eq(MembershipOrderEntity::getCustomerId, current.userId())
                .eq(MembershipOrderEntity::getOrderNo, orderNo)
                .eq(MembershipOrderEntity::getDeleted, 0).last("LIMIT 1"));
        if (order == null) throw new BusinessException(ErrorCode.MEMBERSHIP_ORDER_NOT_FOUND);
        return order;
    }

    private MembershipOrderVo toOrderVo(MembershipOrderEntity order, MembershipPlanEntity plan, boolean simulated) {
        return new MembershipOrderVo(order.getOrderNo(), plan.getPlanCode(), plan.getPlanName(), order.getStatus(),
                order.getAmountCent(), simulated, properties.paymentEnabled(), order.getCreatedAt(), order.getPaidAt(),
                order.getExpireAt());
    }

    private void activateMembership(
            MembershipOrderEntity order, MembershipPlanEntity plan, LocalDateTime now, String source) {
        CustomerMembershipEntity membership = new CustomerMembershipEntity();
        membership.setTenantId(order.getTenantId());
        membership.setCustomerId(order.getCustomerId());
        membership.setPlanId(plan.getId());
        membership.setStatus("ACTIVE");
        membership.setStartAt(now);
        membership.setExpireAt(now.plusDays(plan.getDurationDays()));
        membership.setSource(source);
        membership.setOrderNo(order.getOrderNo());
        membership.setCreatedBy(order.getCustomerId());
        membership.setCreatedAt(now);
        membership.setUpdatedBy(order.getCustomerId());
        membership.setUpdatedAt(now);
        membership.setDeleted(0);
        membership.setVersion(0);
        customerMembershipMapper.insert(membership);
        order.setExpireAt(membership.getExpireAt());
        order.setUpdatedAt(now);
        orderMapper.updateById(order);
    }

}
