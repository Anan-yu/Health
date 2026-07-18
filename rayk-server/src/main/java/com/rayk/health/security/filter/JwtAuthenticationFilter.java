package com.rayk.health.security.filter;

import com.rayk.health.security.jwt.JwtService;
import com.rayk.health.security.service.AuthService;
import com.rayk.health.security.service.CurrentPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    public JwtAuthenticationFilter(JwtService jwtService, StringRedisTemplate redisTemplate) {
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            authenticate(header.substring(7));
        }
        filterChain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private void authenticate(String token) {
        try {
            Claims claims = jwtService.parse(token);
            String workbench = redisTemplate.opsForValue().get(AuthService.sessionKey(claims.getId()));
            if (!StringUtils.hasText(workbench)) {
                return;
            }
            List<String> roles = claims.get("roles", List.class);
            List<String> permissions = claims.get("permissions", List.class);
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            permissions.forEach(item -> authorities.add(new SimpleGrantedAuthority(item)));
            CurrentPrincipal principal =
                    new CurrentPrincipal(
                            claims.getId(),
                            claims.getSubject(),
                            ((Number) claims.get("userId")).longValue(),
                            ((Number) claims.get("tenantId")).longValue(),
                            List.copyOf(roles),
                            List.copyOf(permissions),
                            workbench);
            SecurityContextHolder.getContext()
                    .setAuthentication(
                            new UsernamePasswordAuthenticationToken(principal, null, authorities));
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}

