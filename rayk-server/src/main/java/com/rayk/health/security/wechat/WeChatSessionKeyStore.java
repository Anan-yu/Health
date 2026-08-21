package com.rayk.health.security.wechat;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Stores the current WeChat session key privately for server-side virtual-payment signing. */
@Service
public class WeChatSessionKeyStore {
    private static final String KEY_PREFIX = "rayk:wechat:session-key:";
    private static final Duration SESSION_KEY_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;

    public WeChatSessionKeyStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(WeChatSessionIdentity identity, long userId) {
        if (identity == null || !StringUtils.hasText(identity.sessionKey())) {
            return;
        }
        redisTemplate
                .opsForValue()
                .set(key(identity.appId(), userId), identity.sessionKey(), SESSION_KEY_TTL);
    }

    public String get(String appId, long userId) {
        return redisTemplate.opsForValue().get(key(appId, userId));
    }

    private String key(String appId, long userId) {
        return KEY_PREFIX + appId + ":" + userId;
    }
}
