package com.rayk.health.security.service;

import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import com.rayk.health.security.dto.AuthData;
import com.rayk.health.security.dto.MockLoginRequest;
import com.rayk.health.security.dto.ProfileData;
import com.rayk.health.security.jwt.JwtService;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final String SESSION_PREFIX = "rayk:session:";
    private final MockUserCatalog catalog;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    public AuthService(
            MockUserCatalog catalog,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            StringRedisTemplate redisTemplate) {
        this.catalog = catalog;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
    }

    public AuthData login(MockLoginRequest request) {
        MockAccount account = catalog.require(request.username());
        if (account == null || !passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        return issue(account);
    }

    public AuthData issue(MockAccount account) {
        JwtService.IssuedToken issued =
                jwtService.issue(
                        account.username(),
                        account.userId(),
                        account.tenantId(),
                        account.roles(),
                        account.permissions());
        redisTemplate
                .opsForValue()
                .set(
                        sessionKey(issued.jti()),
                        account.defaultWorkbench(),
                        Duration.ofSeconds(issued.expiresIn()));
        return new AuthData(
                issued.token(),
                "Bearer",
                issued.expiresIn(),
                String.valueOf(account.userId()),
                String.valueOf(account.tenantId()),
                account.tenantName(),
                account.displayName(),
                account.roles(),
                account.permissions(),
                account.workbenches(),
                account.defaultWorkbench());
    }

    public void logout() {
        redisTemplate.delete(sessionKey(CurrentUser.require().jti()));
    }

    public ProfileData profile() {
        CurrentPrincipal principal = CurrentUser.require();
        MockAccount account = catalog.require(principal.username());
        if (account == null) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return new ProfileData(
                String.valueOf(account.userId()),
                String.valueOf(account.tenantId()),
                account.tenantName(),
                account.username(),
                account.displayName(),
                account.roles(),
                account.permissions(),
                account.workbenches(),
                principal.workbench());
    }

    public String switchWorkbench(String code) {
        CurrentPrincipal principal = CurrentUser.require();
        MockAccount account = catalog.require(principal.username());
        boolean allowed = account.workbenches().stream().anyMatch(item -> item.code().equals(code));
        if (!allowed) {
            throw new BusinessException(ErrorCode.WORKBENCH_NOT_ALLOWED);
        }
        Long ttl = redisTemplate.getExpire(sessionKey(principal.jti()));
        Duration duration = Duration.ofSeconds(ttl == null || ttl < 1 ? 7200 : ttl);
        redisTemplate.opsForValue().set(sessionKey(principal.jti()), code, duration);
        return code;
    }

    public static String sessionKey(String jti) {
        return SESSION_PREFIX + jti;
    }
}
