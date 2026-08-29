package com.rayk.health.mall.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.mall.config.MallProperties;
import com.rayk.health.mall.dto.MallAddressRequest;
import com.rayk.health.mall.dto.MallOrderRequest;
import com.rayk.health.mall.dto.MallProductRequest;
import com.rayk.health.mall.entity.MallAddressEntity;
import com.rayk.health.mall.entity.MallOrderEntity;
import com.rayk.health.mall.entity.MallOrderItemEntity;
import com.rayk.health.mall.entity.MallProductEntity;
import com.rayk.health.mall.mapper.MallAddressMapper;
import com.rayk.health.mall.mapper.MallOrderItemMapper;
import com.rayk.health.mall.mapper.MallOrderMapper;
import com.rayk.health.mall.mapper.MallProductMapper;
import com.rayk.health.mall.vo.MallAddressVo;
import com.rayk.health.mall.vo.MallOrderItemVo;
import com.rayk.health.mall.vo.MallOrderVo;
import com.rayk.health.mall.vo.MallPaymentVo;
import com.rayk.health.mall.vo.MallProductVo;
import com.rayk.health.membership.config.MembershipProperties;
import com.rayk.health.membership.payment.WeChatPayClient;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.wechat.entity.WeChatUserBindingEntity;
import com.rayk.health.security.wechat.mapper.WeChatUserBindingMapper;
import com.rayk.health.tenant.TenantContext;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MallApplicationService {
    private static final long CATALOG_TENANT_ID = 1L;
    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MallProperties mallProperties;
    private final MembershipProperties membershipProperties;
    private final MallProductMapper productMapper;
    private final MallAddressMapper addressMapper;
    private final MallOrderMapper orderMapper;
    private final MallOrderItemMapper itemMapper;
    private final WeChatUserBindingMapper bindingMapper;
    private final WeChatPayClient weChatPayClient;

    public MallApplicationService(
            MallProperties mallProperties,
            MembershipProperties membershipProperties,
            MallProductMapper productMapper,
            MallAddressMapper addressMapper,
            MallOrderMapper orderMapper,
            MallOrderItemMapper itemMapper,
            WeChatUserBindingMapper bindingMapper,
            WeChatPayClient weChatPayClient) {
        this.mallProperties = mallProperties;
        this.membershipProperties = membershipProperties;
        this.productMapper = productMapper;
        this.addressMapper = addressMapper;
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.bindingMapper = bindingMapper;
        this.weChatPayClient = weChatPayClient;
    }

    public List<MallProductVo> products(boolean includeInactive) {
        requireMallEnabled();
        CurrentPrincipal current = CurrentUser.require();
        LambdaQueryWrapper<MallProductEntity> query = new LambdaQueryWrapper<MallProductEntity>()
                .eq(MallProductEntity::getTenantId, CATALOG_TENANT_ID)
                .eq(MallProductEntity::getDeleted, 0)
                .orderByAsc(MallProductEntity::getSortOrder)
                .orderByDesc(MallProductEntity::getCreatedAt);
        if (!includeInactive || !current.roles().contains("PLATFORM_ADMIN")) {
            query.eq(MallProductEntity::getStatus, "ACTIVE");
        }
        return productMapper.selectList(query).stream().map(this::toProduct).toList();
    }

    public MallProductVo product(String productId) {
        requireMallEnabled();
        MallProductEntity product = findProduct(productId, false);
        return toProduct(product);
    }

    @Transactional
    public MallProductVo saveProduct(Long productId, @Valid MallProductRequest request) {
        requireMallEnabled();
        CurrentPrincipal admin = requirePlatformAdmin();
        LocalDateTime now = LocalDateTime.now();
        MallProductEntity product = productId == null ? new MallProductEntity() : productMapper.selectById(productId);
        if (productId != null
                && (product == null
                        || !Long.valueOf(CATALOG_TENANT_ID).equals(product.getTenantId())
                        || !Integer.valueOf(0).equals(product.getDeleted()))) {
            throw new BusinessException(ErrorCode.MALL_PRODUCT_NOT_FOUND);
        }
        if (productId == null) {
            product.setId(IdWorker.getId());
            product.setTenantId(CATALOG_TENANT_ID);
            product.setSoldCount(0);
            product.setDeleted(0);
            product.setVersion(0);
            product.setCreatedBy(admin.userId());
            product.setCreatedAt(now);
        }
        product.setProductName(request.productName().trim());
        product.setSubtitle(trimToNull(request.subtitle()));
        product.setDescription(trimToNull(request.description()));
        product.setMainImageUrl(trimToNull(request.mainImageUrl()));
        product.setPriceCent(request.priceCent());
        product.setStock(request.stock());
        product.setStatus(request.status());
        product.setSortOrder(request.sortOrder());
        product.setUpdatedBy(admin.userId());
        product.setUpdatedAt(now);
        if (productId == null) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }
        return toProduct(product);
    }

    @Transactional
    public void disableProduct(String productId) {
        requireMallEnabled();
        CurrentPrincipal admin = requirePlatformAdmin();
        MallProductEntity product = findProduct(productId, true);
        product.setStatus("INACTIVE");
        product.setUpdatedBy(admin.userId());
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);
    }

    public List<MallAddressVo> addresses() {
        requireMallEnabled();
        CurrentPrincipal current = CurrentUser.require();
        return addressMapper.selectList(addressQuery(current))
                .stream().map(this::toAddress).toList();
    }

    @Transactional
    public MallAddressVo saveAddress(String addressId, @Valid MallAddressRequest request) {
        requireMallEnabled();
        CurrentPrincipal current = CurrentUser.require();
        LocalDateTime now = LocalDateTime.now();
        MallAddressEntity address;
        if (StringUtils.hasText(addressId)) {
            address = ownedAddress(addressId, current);
        } else {
            address = new MallAddressEntity();
            address.setId(IdWorker.getId());
            address.setTenantId(current.tenantId());
            address.setCustomerId(current.userId());
            address.setCreatedBy(current.userId());
            address.setCreatedAt(now);
            address.setDeleted(0);
            address.setVersion(0);
        }
        if (request.isDefault() || addressMapper.selectCount(addressQuery(current)) == 0) {
            clearDefault(current, address.getId());
            address.setIsDefault(true);
        } else if (address.getIsDefault() == null) {
            address.setIsDefault(false);
        }
        address.setReceiverName(request.receiverName().trim());
        address.setReceiverPhone(request.receiverPhone().trim());
        address.setProvince(request.province().trim());
        address.setCity(request.city().trim());
        address.setDistrict(request.district().trim());
        address.setDetailAddress(request.detailAddress().trim());
        address.setUpdatedBy(current.userId());
        address.setUpdatedAt(now);
        if (StringUtils.hasText(addressId)) {
            addressMapper.updateById(address);
        } else {
            addressMapper.insert(address);
        }
        return toAddress(address);
    }

    @Transactional
    public void deleteAddress(String addressId) {
        requireMallEnabled();
        CurrentPrincipal current = CurrentUser.require();
        MallAddressEntity address = ownedAddress(addressId, current);
        address.setDeleted(1);
        address.setUpdatedBy(current.userId());
        address.setUpdatedAt(LocalDateTime.now());
        addressMapper.updateById(address);
    }

    @Transactional
    public MallOrderVo createOrder(@Valid MallOrderRequest request) {
        requireMallEnabled();
        CurrentPrincipal current = CurrentUser.require();
        releaseExpiredOrders(current);
        MallProductEntity product = findProduct(request.productId(), false);
        MallAddressEntity address = ownedAddress(request.addressId(), current);
        int total;
        try {
            total = Math.multiplyExact(product.getPriceCent(), request.quantity());
        } catch (ArithmeticException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        LocalDateTime now = LocalDateTime.now();
        if (productMapper.reserveStock(product.getId(), request.quantity(), current.userId(), now) != 1) {
            throw new BusinessException(ErrorCode.MALL_STOCK_NOT_ENOUGH);
        }
        MallOrderEntity order = new MallOrderEntity();
        order.setId(IdWorker.getId());
        order.setTenantId(current.tenantId());
        order.setCustomerId(current.userId());
        order.setOrderNo("G" + now.format(ORDER_TIME) + UUID.randomUUID().toString().replace("-", "").substring(0, 10));
        order.setStatus("PENDING_PAYMENT");
        order.setAmountCent(total);
        order.setExpiresAt(now.plusMinutes(mallProperties.orderExpireMinutes()));
        copyAddress(order, address);
        setAudit(order, current.userId(), now);
        orderMapper.insert(order);

        MallOrderItemEntity item = new MallOrderItemEntity();
        item.setId(IdWorker.getId());
        item.setTenantId(current.tenantId());
        item.setOrderId(order.getId());
        item.setProductId(product.getId());
        item.setProductName(product.getProductName());
        item.setMainImageUrl(product.getMainImageUrl());
        item.setUnitPriceCent(product.getPriceCent());
        item.setQuantity(request.quantity());
        item.setTotalCent(total);
        setAudit(item, current.userId(), now);
        itemMapper.insert(item);
        return toOrder(order, List.of(item));
    }

    public List<MallOrderVo> orders() {
        requireMallEnabled();
        CurrentPrincipal current = CurrentUser.require();
        releaseExpiredOrders(current);
        return orderMapper.selectList(orderQuery(current).orderByDesc(MallOrderEntity::getCreatedAt))
                .stream().map(order -> toOrder(order, items(order.getId(), current.tenantId()))).toList();
    }

    public MallOrderVo order(String orderNo) {
        requireMallEnabled();
        CurrentPrincipal current = CurrentUser.require();
        releaseExpiredOrders(current);
        MallOrderEntity order = ownedOrder(orderNo, current);
        return toOrder(order, items(order.getId(), current.tenantId()));
    }

    @Transactional
    public void cancelOrder(String orderNo) {
        requireMallEnabled();
        CurrentPrincipal current = CurrentUser.require();
        MallOrderEntity order = ownedOrder(orderNo, current);
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.MALL_ORDER_INVALID_STATUS);
        }
        LocalDateTime now = LocalDateTime.now();
        int changed = orderMapper.update(null, new LambdaUpdateWrapper<MallOrderEntity>()
                .eq(MallOrderEntity::getId, order.getId())
                .eq(MallOrderEntity::getTenantId, current.tenantId())
                .eq(MallOrderEntity::getCustomerId, current.userId())
                .eq(MallOrderEntity::getStatus, "PENDING_PAYMENT")
                .set(MallOrderEntity::getStatus, "CANCELLED")
                .set(MallOrderEntity::getCancelReason, "客户取消")
                .set(MallOrderEntity::getUpdatedBy, current.userId())
                .set(MallOrderEntity::getUpdatedAt, now));
        if (changed != 1) {
            throw new BusinessException(ErrorCode.MALL_ORDER_INVALID_STATUS);
        }
        releaseStockForOrder(order, current.userId(), now);
    }

    public MallPaymentVo createWechatPayment(String orderNo) {
        requireMallEnabled();
        if (!mallProperties.paymentEnabled() || !membershipProperties.wechatPay().configured()
                || !weChatPayClient.configured()) {
            throw new BusinessException(ErrorCode.MALL_PAYMENT_NOT_CONFIGURED);
        }
        CurrentPrincipal current = CurrentUser.require();
        MallOrderEntity order = ownedOrder(orderNo, current);
        if (!"PENDING_PAYMENT".equals(order.getStatus())
                || order.getExpiresAt() == null || !order.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.MALL_ORDER_INVALID_STATUS);
        }
        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        WeChatUserBindingEntity binding = bindingMapper.selectOne(new LambdaQueryWrapper<WeChatUserBindingEntity>()
                .eq(WeChatUserBindingEntity::getTenantId, current.tenantId())
                .eq(WeChatUserBindingEntity::getUserId, current.userId())
                .eq(WeChatUserBindingEntity::getAppId, pay.appId())
                .eq(WeChatUserBindingEntity::getStatus, "ACTIVE")
                .eq(WeChatUserBindingEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (binding == null || !StringUtils.hasText(binding.getOpenid())) {
            throw new BusinessException(ErrorCode.WECHAT_PAYMENT_SESSION_EXPIRED);
        }
        Amount amount = new Amount();
        amount.setTotal(order.getAmountCent());
        amount.setCurrency("CNY");
        Payer payer = new Payer();
        payer.setOpenid(binding.getOpenid());
        PrepayRequest request = new PrepayRequest();
        request.setAppid(pay.appId());
        request.setMchid(pay.merchantId());
        request.setDescription("三羊健康商城-实物商品");
        request.setOutTradeNo(order.getOrderNo());
        request.setNotifyUrl(pay.notifyUrl());
        request.setAmount(amount);
        request.setPayer(payer);
        try {
            PrepayWithRequestPaymentResponse response = weChatPayClient.prepay(request);
            return new MallPaymentVo(
                    response.getTimeStamp(), response.getNonceStr(), response.getPackageVal(),
                    response.getSignType(), response.getPaySign());
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.MEMBERSHIP_PAYMENT_NOT_CONFIGURED) {
                throw new BusinessException(ErrorCode.MALL_PAYMENT_NOT_CONFIGURED);
            }
            if (exception.getErrorCode() == ErrorCode.MEMBERSHIP_PAYMENT_AUTH_REQUIRED) {
                throw new BusinessException(ErrorCode.MALL_PAYMENT_AUTH_REQUIRED);
            }
            throw new BusinessException(ErrorCode.MALL_PAYMENT_UNAVAILABLE);
        }
    }

    /** Dispatches a verified standard WeChat Pay notification for G-prefixed goods orders. */
    @Transactional
    public void handleWechatPayment(Transaction transaction) {
        TenantContext.executeReadWithoutTenant(() -> {
            handleWechatPaymentWithoutTenant(transaction);
            return null;
        });
    }

    private void handleWechatPaymentWithoutTenant(Transaction transaction) {
        if (transaction == null || !StringUtils.hasText(transaction.getOutTradeNo())) {
            throw new BusinessException(ErrorCode.MALL_ORDER_NOT_FOUND);
        }
        MallOrderEntity order = orderMapper.selectOne(new LambdaQueryWrapper<MallOrderEntity>()
                .eq(MallOrderEntity::getOrderNo, transaction.getOutTradeNo())
                .eq(MallOrderEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.MALL_ORDER_NOT_FOUND);
        }
        if ("PAID".equals(order.getStatus())) {
            return;
        }
        MembershipProperties.WeChatPayProperties pay = membershipProperties.wechatPay();
        Integer amount = transaction.getAmount() == null ? null : transaction.getAmount().getTotal();
        if (!mallProperties.paymentEnabled() || !pay.configured()
                || !pay.appId().equals(transaction.getAppid())
                || !pay.merchantId().equals(transaction.getMchid())
                || transaction.getTradeState() != Transaction.TradeStateEnum.SUCCESS
                || amount == null || amount.intValue() != order.getAmountCent()
                || !StringUtils.hasText(transaction.getTransactionId())) {
            throw new BusinessException(ErrorCode.MALL_PAYMENT_UNAVAILABLE);
        }
        MallOrderEntity duplicate = orderMapper.selectOne(new LambdaQueryWrapper<MallOrderEntity>()
                .eq(MallOrderEntity::getTransactionId, transaction.getTransactionId())
                .ne(MallOrderEntity::getOrderNo, order.getOrderNo())
                .eq(MallOrderEntity::getDeleted, 0)
                .last("LIMIT 1"));
        if (duplicate != null) {
            throw new BusinessException(ErrorCode.MALL_PAYMENT_UNAVAILABLE);
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
    }

    private void releaseExpiredOrders(CurrentPrincipal current) {
        LocalDateTime now = LocalDateTime.now();
        List<MallOrderEntity> expired = orderMapper.selectList(orderQuery(current)
                .eq(MallOrderEntity::getStatus, "PENDING_PAYMENT")
                .le(MallOrderEntity::getExpiresAt, now)
                .last("LIMIT 100"));
        for (MallOrderEntity order : expired) {
            int changed = orderMapper.update(null, new LambdaUpdateWrapper<MallOrderEntity>()
                    .eq(MallOrderEntity::getId, order.getId())
                    .eq(MallOrderEntity::getStatus, "PENDING_PAYMENT")
                    .set(MallOrderEntity::getStatus, "CANCELLED")
                    .set(MallOrderEntity::getCancelReason, "订单超时未支付")
                    .set(MallOrderEntity::getUpdatedBy, current.userId())
                    .set(MallOrderEntity::getUpdatedAt, now));
            if (changed == 1) {
                releaseStockForOrder(order, current.userId(), now);
            }
        }
    }

    private void releaseStockForOrder(MallOrderEntity order, long operatorId, LocalDateTime now) {
        for (MallOrderItemEntity item : items(order.getId(), order.getTenantId())) {
            productMapper.releaseStock(item.getProductId(), item.getQuantity(), operatorId, now);
        }
    }

    private MallProductEntity findProduct(String productId, boolean includeInactive) {
        long id = parseId(productId, ErrorCode.MALL_PRODUCT_NOT_FOUND);
        MallProductEntity product = productMapper.selectOne(new LambdaQueryWrapper<MallProductEntity>()
                .eq(MallProductEntity::getId, id)
                .eq(MallProductEntity::getTenantId, CATALOG_TENANT_ID)
                .eq(MallProductEntity::getDeleted, 0)
                .eq(!includeInactive, MallProductEntity::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        if (product == null) {
            throw new BusinessException(ErrorCode.MALL_PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private MallAddressEntity ownedAddress(String addressId, CurrentPrincipal current) {
        long id = parseId(addressId, ErrorCode.MALL_ADDRESS_NOT_FOUND);
        MallAddressEntity address = addressMapper.selectOne(addressQuery(current)
                .eq(MallAddressEntity::getId, id).last("LIMIT 1"));
        if (address == null) {
            throw new BusinessException(ErrorCode.MALL_ADDRESS_NOT_FOUND);
        }
        return address;
    }

    private MallOrderEntity ownedOrder(String orderNo, CurrentPrincipal current) {
        MallOrderEntity order = orderMapper.selectOne(orderQuery(current)
                .eq(MallOrderEntity::getOrderNo, orderNo).last("LIMIT 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.MALL_ORDER_NOT_FOUND);
        }
        return order;
    }

    private List<MallOrderItemEntity> items(Long orderId, Long tenantId) {
        return itemMapper.selectList(new LambdaQueryWrapper<MallOrderItemEntity>()
                .eq(MallOrderItemEntity::getTenantId, tenantId)
                .eq(MallOrderItemEntity::getOrderId, orderId)
                .eq(MallOrderItemEntity::getDeleted, 0));
    }

    private void requireMallEnabled() {
        if (!mallProperties.enabled()) {
            throw new BusinessException(ErrorCode.MALL_NOT_ENABLED);
        }
    }

    private LambdaQueryWrapper<MallAddressEntity> addressQuery(CurrentPrincipal current) {
        return new LambdaQueryWrapper<MallAddressEntity>()
                .eq(MallAddressEntity::getTenantId, current.tenantId())
                .eq(MallAddressEntity::getCustomerId, current.userId())
                .eq(MallAddressEntity::getDeleted, 0)
                .orderByDesc(MallAddressEntity::getIsDefault)
                .orderByDesc(MallAddressEntity::getUpdatedAt);
    }

    private LambdaQueryWrapper<MallOrderEntity> orderQuery(CurrentPrincipal current) {
        return new LambdaQueryWrapper<MallOrderEntity>()
                .eq(MallOrderEntity::getTenantId, current.tenantId())
                .eq(MallOrderEntity::getCustomerId, current.userId())
                .eq(MallOrderEntity::getDeleted, 0);
    }

    private void clearDefault(CurrentPrincipal current, Long keepId) {
        List<MallAddressEntity> addresses = addressMapper.selectList(addressQuery(current));
        for (MallAddressEntity address : addresses) {
            if (!address.getId().equals(keepId) && Boolean.TRUE.equals(address.getIsDefault())) {
                address.setIsDefault(false);
                address.setUpdatedBy(current.userId());
                address.setUpdatedAt(LocalDateTime.now());
                addressMapper.updateById(address);
            }
        }
    }

    private void copyAddress(MallOrderEntity order, MallAddressEntity address) {
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setProvince(address.getProvince());
        order.setCity(address.getCity());
        order.setDistrict(address.getDistrict());
        order.setDetailAddress(address.getDetailAddress());
    }

    private void setAudit(MallOrderEntity entity, long userId, LocalDateTime now) {
        entity.setCreatedBy(userId);
        entity.setCreatedAt(now);
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(now);
        entity.setDeleted(0);
        entity.setVersion(0);
    }

    private void setAudit(MallOrderItemEntity entity, long userId, LocalDateTime now) {
        entity.setCreatedBy(userId);
        entity.setCreatedAt(now);
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(now);
        entity.setDeleted(0);
        entity.setVersion(0);
    }

    private MallProductVo toProduct(MallProductEntity product) {
        return new MallProductVo(String.valueOf(product.getId()), product.getProductName(), product.getSubtitle(),
                product.getDescription(), product.getMainImageUrl(), product.getPriceCent(), product.getStock(),
                product.getSoldCount(), product.getStatus(), product.getSortOrder());
    }

    private MallAddressVo toAddress(MallAddressEntity address) {
        return new MallAddressVo(String.valueOf(address.getId()), address.getReceiverName(), address.getReceiverPhone(),
                address.getProvince(), address.getCity(), address.getDistrict(), address.getDetailAddress(),
                Boolean.TRUE.equals(address.getIsDefault()));
    }

    private MallOrderVo toOrder(MallOrderEntity order, List<MallOrderItemEntity> items) {
        return new MallOrderVo(order.getOrderNo(), order.getStatus(), order.getAmountCent(), order.getPaymentChannel(),
                order.getTransactionId(), order.getReceiverName(), order.getReceiverPhone(), order.getProvince(),
                order.getCity(), order.getDistrict(), order.getDetailAddress(), order.getExpiresAt(),
                order.getCreatedAt(), order.getPaidAt(), items.stream().map(item -> new MallOrderItemVo(
                        String.valueOf(item.getProductId()), item.getProductName(), item.getMainImageUrl(),
                        item.getUnitPriceCent(), item.getQuantity(), item.getTotalCent())).toList());
    }

    private CurrentPrincipal requirePlatformAdmin() {
        CurrentPrincipal current = CurrentUser.require();
        if (!current.roles().contains("PLATFORM_ADMIN")) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        return current;
    }

    private long parseId(String value, ErrorCode errorCode) {
        try {
            return Long.parseLong(value);
        } catch (Exception exception) {
            throw new BusinessException(errorCode);
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
