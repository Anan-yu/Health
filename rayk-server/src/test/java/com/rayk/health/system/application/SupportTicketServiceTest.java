package com.rayk.health.system.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.system.dto.CreateSupportTicketRequest;
import com.rayk.health.system.dto.ReplySupportTicketRequest;
import com.rayk.health.system.entity.SupportTicketEntity;
import com.rayk.health.system.mapper.SupportTicketMapper;
import com.rayk.health.system.vo.SupportTicketVo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SupportTicketServiceTest {
    private SupportTicketMapper ticketMapper;
    private SupportTicketService service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                SupportTicketEntity.class);
    }

    @BeforeEach
    void setUp() {
        ticketMapper = mock(SupportTicketMapper.class);
        service = new SupportTicketService(ticketMapper);

        CurrentPrincipal principal =
                new CurrentPrincipal(
                        "support-test",
                        "customer",
                        10005L,
                        20001L,
                        List.of("CUSTOMER"),
                        List.of("self:health-record"),
                        "CUSTOMER");
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsTicketForCurrentTenantAndUserAndNormalizesInput() {
        doAnswer(
                        invocation -> {
                            SupportTicketEntity entity = invocation.getArgument(0);
                            entity.setId(2079545597569081345L);
                            return 1;
                        })
                .when(ticketMapper)
                .insert(any(SupportTicketEntity.class));

        SupportTicketVo created =
                service.create(new CreateSupportTicketRequest(" BUG ", " 页面无法提交 ", "  "));

        ArgumentCaptor<SupportTicketEntity> captor =
                ArgumentCaptor.forClass(SupportTicketEntity.class);
        verify(ticketMapper).insert(captor.capture());
        SupportTicketEntity stored = captor.getValue();
        assertThat(stored.getTenantId()).isEqualTo(20001L);
        assertThat(stored.getUserId()).isEqualTo(10005L);
        assertThat(stored.getCategory()).isEqualTo("BUG");
        assertThat(stored.getContent()).isEqualTo("页面无法提交");
        assertThat(stored.getContact()).isNull();
        assertThat(stored.getStatus()).isEqualTo("OPEN");
        assertThat(created.id()).isEqualTo("2079545597569081345");
    }

    @Test
    void returnsOnlyResolvedTicketsWithAPlatformReply() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 21, 20, 34, 41);
        SupportTicketEntity entity = new SupportTicketEntity();
        entity.setId(2079545597569081345L);
        entity.setTenantId(20001L);
        entity.setUserId(10005L);
        entity.setCategory("OTHER");
        entity.setContent("联调反馈");
        entity.setStatus("RESOLVED");
        entity.setReply("已回复，请刷新后重试");
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        entity.setDeleted(0);
        when(ticketMapper.selectList(any())).thenReturn(List.of(entity));

        List<SupportTicketVo> tickets = service.listMine();

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).id()).isEqualTo("2079545597569081345");
        assertThat(tickets.get(0).content()).isEqualTo("联调反馈");
    }

    @Test
    void rejectsPlatformAdministratorTicketCreation() {
        CurrentPrincipal platform =
                new CurrentPrincipal(
                        "platform-test",
                        "platform_admin",
                        10001L,
                        1L,
                        List.of("PLATFORM_ADMIN"),
                        List.of("platform:audit:list"),
                        "PLATFORM_ADMIN");
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(platform, null));

        assertThatThrownBy(
                        () ->
                                service.create(
                                        new CreateSupportTicketRequest("BUG", "不应允许平台提交", null)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void platformAdministratorCanReplyAcrossTenants() {
        CurrentPrincipal platform =
                new CurrentPrincipal(
                        "platform-test",
                        "platform_admin",
                        10001L,
                        1L,
                        List.of("PLATFORM_ADMIN"),
                        List.of("platform:audit:list"),
                        "PLATFORM_ADMIN");
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(platform, null));
        SupportTicketEntity entity = new SupportTicketEntity();
        entity.setId(2079545597569081345L);
        entity.setTenantId(20001L);
        entity.setUserId(10005L);
        entity.setCategory("BUG");
        entity.setContent("机构端异常");
        entity.setStatus("OPEN");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        when(ticketMapper.selectPlatformTicketById(entity.getId())).thenReturn(entity);
        when(ticketMapper.updatePlatformReply(any(Long.class), any(), any(), any(Long.class), any()))
                .thenReturn(1);

        var replied =
                service.replyForPlatform(
                        entity.getId(), new ReplySupportTicketRequest("已处理，请刷新后重试", "RESOLVED"));

        assertThat(replied.tenantId()).isEqualTo("20001");
        assertThat(replied.reply()).isEqualTo("已处理，请刷新后重试");
        assertThat(replied.status()).isEqualTo("RESOLVED");
    }
}
