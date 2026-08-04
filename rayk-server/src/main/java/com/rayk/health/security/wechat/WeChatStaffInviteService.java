package com.rayk.health.security.wechat;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.security.dto.WeChatStaffInviteData;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.security.service.UserAccount;
import com.rayk.health.security.service.UserCatalog;
import java.security.SecureRandom;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** Issues short-lived staff binding codes without exposing a staff phone number to the client. */
@Service
public class WeChatStaffInviteService {
    private static final String KEY_PREFIX = "rayk:wechat:staff-invite:";
    private static final long EXPIRES_IN_SECONDS = 900L;
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final UserCatalog catalog;

    public WeChatStaffInviteService(StringRedisTemplate redisTemplate, UserCatalog catalog) {
        this.redisTemplate = redisTemplate;
        this.catalog = catalog;
    }

    public WeChatStaffInviteData create(long tenantId, long userId) {
        if (!CurrentUser.require().roles().contains("PLATFORM_ADMIN")) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        UserAccount account = catalog.findByUserId(userId);
        if (account == null
                || !account.isActive()
                || account.tenantId() != tenantId
                || !account.roles().contains("DOCTOR")) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        String code = randomCode();
        redisTemplate.opsForValue().set(key(code), String.valueOf(userId), Duration.ofSeconds(EXPIRES_IN_SECONDS));
        return new WeChatStaffInviteData(code, EXPIRES_IN_SECONDS);
    }

    public long consume(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.WECHAT_ACCOUNT_NOT_BOUND);
        }
        String normalized = code.trim().toUpperCase();
        String userId = redisTemplate.opsForValue().getAndDelete(key(normalized));
        if (userId == null) {
            throw new BusinessException(ErrorCode.WECHAT_ACCOUNT_NOT_BOUND);
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.WECHAT_ACCOUNT_NOT_BOUND);
        }
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(10);
        for (int index = 0; index < 10; index++) {
            code.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
        }
        return code.toString();
    }

    private String key(String code) {
        return KEY_PREFIX + code;
    }
}
