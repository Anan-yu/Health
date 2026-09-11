package com.rayk.health.goldbean.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rayk.health.common.api.PageResponse;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.dto.CreateGoldBeanPlatformInviteRequest;
import com.rayk.health.goldbean.entity.GoldBeanOrderEntity;
import com.rayk.health.goldbean.entity.GoldBeanPlatformInviteEntity;
import com.rayk.health.goldbean.mapper.GoldBeanOrderMapper;
import com.rayk.health.goldbean.mapper.GoldBeanPlatformInviteMapper;
import com.rayk.health.platform.vo.PlatformGoldBeanInviteCreatedVo;
import com.rayk.health.platform.vo.PlatformGoldBeanInviteVo;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.wechat.PhoneIdentity;
import com.rayk.health.system.entity.SysUserEntity;
import com.rayk.health.system.mapper.SysUserMapper;
import com.rayk.health.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Issues and atomically consumes one-time codes for the platform's first member. */
@Service
public class GoldBeanPlatformInviteService {
    static final String AVAILABLE = "AVAILABLE";
    static final String RESERVED = "RESERVED";
    static final String CONSUMED = "CONSUMED";
    static final String REVOKED = "REVOKED";
    static final String EXPIRED = "EXPIRED";
    static final String PLATFORM_ROOT_SLOT = "PLATFORM_ROOT";

    private static final String CODE_PREFIX = "GBROOT-";
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String CODE_MASKED = CODE_PREFIX + "••••••••";
    private static final int CODE_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GoldBeanProperties properties;
    private final GoldBeanPlatformInviteMapper inviteMapper;
    private final GoldBeanOrderMapper orderMapper;
    private final SysUserMapper userMapper;

    public GoldBeanPlatformInviteService(
            GoldBeanProperties properties,
            GoldBeanPlatformInviteMapper inviteMapper,
            GoldBeanOrderMapper orderMapper,
            SysUserMapper userMapper) {
        this.properties = properties;
        this.inviteMapper = inviteMapper;
        this.orderMapper = orderMapper;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformGoldBeanInviteVo> list(String status) {
        requirePlatformAdmin();
        String normalizedStatus = normalizeStatus(status);
        LocalDateTime now = LocalDateTime.now();
        var records = inviteMapper.selectList(new LambdaQueryWrapper<GoldBeanPlatformInviteEntity>()
                        .eq(GoldBeanPlatformInviteEntity::getDeleted, 0)
                        .orderByDesc(GoldBeanPlatformInviteEntity::getCreatedAt))
                .stream()
                .map(item -> toVo(item, now))
                .filter(item -> normalizedStatus.isBlank()
                        || "ALL".equals(normalizedStatus)
                        || normalizedStatus.equals(item.status()))
                .limit(200)
                .toList();
        return new PageResponse<>(records, records.size(), 1, Math.max(records.size(), 1));
    }

    @Transactional
    public PlatformGoldBeanInviteCreatedVo issue(CreateGoldBeanPlatformInviteRequest request) {
        CurrentPrincipal admin = requirePlatformAdmin();
        if (hasPlatformRegistration() || hasActivePlatformRegistrationSlot()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_REGISTRATION_ONLY);
        }

        String boundPhone = normalizeBoundPhone(request == null ? null : request.boundPhone());
        String boundPhoneHash = boundPhone.isBlank() ? null : PhoneIdentity.hash(boundPhone);
        String boundPhoneMasked = boundPhone.isBlank() ? null : PhoneIdentity.mask(boundPhone);
        int validHours = request == null || request.validHours() == null ? 24 : request.validHours();
        if (validHours < 1 || validHours > 168) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }

        LocalDateTime now = LocalDateTime.now();
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = generateCode();
            GoldBeanPlatformInviteEntity invite = new GoldBeanPlatformInviteEntity();
            invite.setId(IdWorker.getId());
            invite.setInviteCodeHash(hashCode(code));
            invite.setStatus(AVAILABLE);
            invite.setBoundPhoneMasked(boundPhoneMasked);
            invite.setBoundPhoneHash(boundPhoneHash);
            invite.setExpiresAt(now.plusHours(validHours));
            invite.setCreatedBy(admin.userId());
            invite.setCreatedAt(now);
            invite.setUpdatedBy(admin.userId());
            invite.setUpdatedAt(now);
            invite.setDeleted(0);
            invite.setVersion(0);
            try {
                inviteMapper.insert(invite);
                return new PlatformGoldBeanInviteCreatedVo(
                        String.valueOf(invite.getId()),
                        code,
                        CODE_MASKED,
                        invite.getStatus(),
                        invite.getBoundPhoneMasked(),
                        invite.getExpiresAt());
            } catch (DuplicateKeyException exception) {
                // A random-code collision is exceptionally unlikely; retry without exposing it.
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR);
    }

    @Transactional
    public PlatformGoldBeanInviteVo revoke(String idText) {
        CurrentPrincipal admin = requirePlatformAdmin();
        long id;
        try {
            id = Long.parseLong(idText == null ? "" : idText.trim());
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_INVITE_NOT_FOUND);
        }
        GoldBeanPlatformInviteEntity invite = inviteMapper.selectOne(
                new LambdaQueryWrapper<GoldBeanPlatformInviteEntity>()
                        .eq(GoldBeanPlatformInviteEntity::getId, id)
                        .eq(GoldBeanPlatformInviteEntity::getDeleted, 0)
                        .last("LIMIT 1 FOR UPDATE"));
        if (invite == null) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_INVITE_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        String effectiveStatus = effectiveStatus(invite, now);
        if (!AVAILABLE.equals(effectiveStatus)) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_INVITE_INVALID);
        }
        invite.setStatus(REVOKED);
        invite.setRevokedAt(now);
        invite.setUpdatedBy(admin.userId());
        invite.setUpdatedAt(now);
        inviteMapper.updateById(invite);
        return toVo(invite, now);
    }

    /** Reserves a code for an order while the caller's transaction is still open. */
    @Transactional
    GoldBeanPlatformInviteEntity reserve(String code, CurrentPrincipal customer, String orderNo) {
        String normalized = normalizeCode(code);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_INVITE_REQUIRED);
        }
        GoldBeanPlatformInviteEntity invite = inviteMapper.selectOne(
                new LambdaQueryWrapper<GoldBeanPlatformInviteEntity>()
                        .eq(GoldBeanPlatformInviteEntity::getInviteCodeHash, hashCode(normalized))
                        .eq(GoldBeanPlatformInviteEntity::getDeleted, 0)
                        .last("LIMIT 1 FOR UPDATE"));
        if (invite == null) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_INVITE_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        if (isExpired(invite, now)) {
            expire(invite, now, customer.userId());
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_INVITE_INVALID);
        }
        if (!AVAILABLE.equals(invite.getStatus())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_INVITE_INVALID);
        }
        verifyBinding(invite, customer);
        invite.setStatus(RESERVED);
        invite.setReservedOrderNo(orderNo);
        invite.setUpdatedBy(customer.userId());
        invite.setUpdatedAt(now);
        inviteMapper.updateById(invite);
        return invite;
    }

    /** Consumes the reserved code after membership delivery has succeeded. */
    @Transactional
    void consume(Long inviteId, String orderNo, long operatorId) {
        if (inviteId == null || !StringUtils.hasText(orderNo)) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_INVITE_INVALID);
        }
        GoldBeanPlatformInviteEntity invite = lockedInvite(inviteId);
        if (invite == null) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_INVITE_INVALID);
        }
        if (CONSUMED.equals(invite.getStatus()) && orderNo.equals(invite.getConsumedOrderNo())) {
            return;
        }
        if (!RESERVED.equals(invite.getStatus()) || !orderNo.equals(invite.getReservedOrderNo())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_INVITE_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        invite.setStatus(CONSUMED);
        invite.setReservedOrderNo(null);
        invite.setConsumedOrderNo(orderNo);
        invite.setConsumedAt(now);
        invite.setUpdatedBy(operatorId);
        invite.setUpdatedAt(now);
        inviteMapper.updateById(invite);
    }

    /** Releases a reservation when its order is cancelled or expires. */
    @Transactional
    void release(Long inviteId, String orderNo, long operatorId) {
        if (inviteId == null || !StringUtils.hasText(orderNo)) return;
        GoldBeanPlatformInviteEntity invite = lockedInvite(inviteId);
        if (invite == null
                || !RESERVED.equals(invite.getStatus())
                || !orderNo.equals(invite.getReservedOrderNo())) return;
        LocalDateTime now = LocalDateTime.now();
        invite.setStatus(isExpired(invite, now) ? EXPIRED : AVAILABLE);
        invite.setReservedOrderNo(null);
        invite.setUpdatedBy(operatorId);
        invite.setUpdatedAt(now);
        inviteMapper.updateById(invite);
    }

    private GoldBeanPlatformInviteEntity lockedInvite(long id) {
        return inviteMapper.selectOne(new LambdaQueryWrapper<GoldBeanPlatformInviteEntity>()
                .eq(GoldBeanPlatformInviteEntity::getId, id)
                .eq(GoldBeanPlatformInviteEntity::getDeleted, 0)
                .last("LIMIT 1 FOR UPDATE"));
    }

    private void verifyBinding(GoldBeanPlatformInviteEntity invite, CurrentPrincipal customer) {
        if (!StringUtils.hasText(invite.getBoundPhoneHash())) return;
        SysUserEntity user = userMapper.selectByIdIgnoringTenant(customer.userId());
        if (user == null || !invite.getBoundPhoneHash().equals(user.getPhoneHash())) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_PLATFORM_INVITE_BOUND);
        }
    }

    private void expire(GoldBeanPlatformInviteEntity invite, LocalDateTime now, long operatorId) {
        invite.setStatus(EXPIRED);
        invite.setReservedOrderNo(null);
        invite.setUpdatedBy(operatorId);
        invite.setUpdatedAt(now);
        inviteMapper.updateById(invite);
    }

    private CurrentPrincipal requirePlatformAdmin() {
        CurrentPrincipal current = CurrentUser.require();
        if (!current.roles().contains("PLATFORM_ADMIN")) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        if (!properties.enabled()) {
            throw new BusinessException(ErrorCode.GOLD_BEAN_NOT_ENABLED);
        }
        return current;
    }

    private boolean hasPlatformRegistration() {
        long count = TenantContext.executeReadWithoutTenant(() -> orderMapper.selectCount(
                new LambdaQueryWrapper<GoldBeanOrderEntity>()
                        .ne(GoldBeanOrderEntity::getTenantId, 1L)
                        .eq(GoldBeanOrderEntity::getOrderType, "REGISTRATION_FEE")
                        .eq(GoldBeanOrderEntity::getStatus, "PAID")
                        .eq(GoldBeanOrderEntity::getRegistrationFeeRecipient, "PLATFORM")
                        .in(GoldBeanOrderEntity::getPaymentChannel, "WECHAT_VIRTUAL", "DEVELOPMENT_RECORD")
                        .eq(GoldBeanOrderEntity::getDeleted, 0)));
        return count > 0;
    }

    private boolean hasActivePlatformRegistrationSlot() {
        LocalDateTime now = LocalDateTime.now();
        long count = TenantContext.executeReadWithoutTenant(() -> orderMapper.selectCount(
                new LambdaQueryWrapper<GoldBeanOrderEntity>()
                        .ne(GoldBeanOrderEntity::getTenantId, 1L)
                        .eq(GoldBeanOrderEntity::getPlatformSlotKey, GoldBeanPlatformInviteService.PLATFORM_ROOT_SLOT)
                        .eq(GoldBeanOrderEntity::getDeleted, 0)
                        .and(wrapper -> wrapper.eq(GoldBeanOrderEntity::getStatus, "PAID")
                                .or(item -> item.eq(GoldBeanOrderEntity::getStatus, "PENDING")
                                        .gt(GoldBeanOrderEntity::getExpiresAt, now)))));
        return count > 0;
    }

    private PlatformGoldBeanInviteVo toVo(GoldBeanPlatformInviteEntity invite, LocalDateTime now) {
        return new PlatformGoldBeanInviteVo(
                String.valueOf(invite.getId()),
                CODE_MASKED,
                effectiveStatus(invite, now),
                invite.getBoundPhoneMasked(),
                invite.getReservedOrderNo(),
                invite.getConsumedOrderNo(),
                invite.getExpiresAt(),
                invite.getCreatedAt(),
                invite.getConsumedAt(),
                invite.getRevokedAt());
    }

    private String effectiveStatus(GoldBeanPlatformInviteEntity invite, LocalDateTime now) {
        if ((AVAILABLE.equals(invite.getStatus()) || RESERVED.equals(invite.getStatus()))
                && isExpired(invite, now)) return EXPIRED;
        return invite.getStatus();
    }

    private boolean isExpired(GoldBeanPlatformInviteEntity invite, LocalDateTime now) {
        return invite.getExpiresAt() != null && !now.isBefore(invite.getExpiresAt());
    }

    private static String normalizeBoundPhone(String value) {
        if (!StringUtils.hasText(value)) return "";
        try {
            return PhoneIdentity.normalize(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
    }

    private static String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeStatus(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String generateCode() {
        StringBuilder code = new StringBuilder(CODE_PREFIX);
        for (int index = 0; index < CODE_LENGTH; index++) {
            code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private static String hashCode(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
