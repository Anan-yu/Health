package com.rayk.health.goldbean.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rayk.health.goldbean.config.GoldBeanProperties;
import com.rayk.health.goldbean.entity.GoldBeanAccountEntity;
import com.rayk.health.goldbean.entity.GoldBeanLegendaryEntity;
import com.rayk.health.goldbean.mapper.GoldBeanAccountMapper;
import com.rayk.health.goldbean.mapper.GoldBeanLegendaryMapper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.wechat.PhoneIdentity;
import com.rayk.health.system.entity.SysUserEntity;
import com.rayk.health.system.mapper.SysUserMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class GoldBeanLegendaryServiceTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void matchesOnlyAnActivePlatformPhoneAllowlistEntry() {
        GoldBeanLegendaryMapper legendaryMapper = mock(GoldBeanLegendaryMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        GoldBeanLegendaryService service = new GoldBeanLegendaryService(
                new GoldBeanProperties(true, true, 60, 60, 20, 100, 99_800, 7, 50,
                        true, "normal_member_998", "gold_bean", 100, 10_000,
                        "机器人权益服务群", ""),
                legendaryMapper,
                mock(GoldBeanAccountMapper.class),
                userMapper);
        String phoneHash = PhoneIdentity.hash("13800138000");
        SysUserEntity user = new SysUserEntity();
        user.setId(3L);
        user.setPhoneHash(phoneHash);
        GoldBeanLegendaryEntity allowlist = new GoldBeanLegendaryEntity();
        allowlist.setStatus("ACTIVE");
        when(userMapper.selectByIdIgnoringTenant(3L)).thenReturn(user);
        when(legendaryMapper.selectActiveByPhoneHash(phoneHash)).thenReturn(allowlist);

        assertThat(service.isEligible(3L)).isTrue();

        when(legendaryMapper.selectActiveByPhoneHash(phoneHash)).thenReturn(null);
        assertThat(service.isEligible(3L)).isFalse();
    }

    @Test
    void listsMatchedUsersWithTheirDigitalBankBalance() {
        GoldBeanLegendaryMapper legendaryMapper = mock(GoldBeanLegendaryMapper.class);
        GoldBeanAccountMapper accountMapper = mock(GoldBeanAccountMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        GoldBeanLegendaryService service = new GoldBeanLegendaryService(
                new GoldBeanProperties(true, true, 60, 60, 20, 100, 99_800, 7, 50,
                        true, "normal_member_998", "gold_bean", 100, 10_000,
                        "机器人权益服务群", ""),
                legendaryMapper,
                accountMapper,
                userMapper);

        String phoneHash = PhoneIdentity.hash("15038673671");
        GoldBeanLegendaryEntity allowlist = new GoldBeanLegendaryEntity();
        allowlist.setId(7L);
        allowlist.setPhoneHash(phoneHash);
        allowlist.setPhoneMasked("150****3671");
        allowlist.setStatus("ACTIVE");

        SysUserEntity user = new SysUserEntity();
        user.setId(3L);
        user.setDisplayName("测试传奇");
        user.setPhoneHash(phoneHash);

        GoldBeanAccountEntity account = new GoldBeanAccountEntity();
        account.setUserId(3L);
        account.setDigitalBankBalance(BigDecimal.valueOf(66));
        account.setDeleted(0);

        when(legendaryMapper.selectPlatformList()).thenReturn(List.of(allowlist));
        when(userMapper.selectAllIgnoringTenant()).thenReturn(List.of(user));
        when(accountMapper.selectOne(any())).thenReturn(account);
        authenticateAsPlatformAdmin();

        var result = service.list();

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).digitalBankBalance()).isEqualByComparingTo(BigDecimal.valueOf(66));
    }

    private void authenticateAsPlatformAdmin() {
        CurrentPrincipal principal = new CurrentPrincipal(
                "jti", "admin", 1L, 1L, List.of("PLATFORM_ADMIN"), List.of(), "PLATFORM");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
