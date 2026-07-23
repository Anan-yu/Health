package com.rayk.health.system.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.system.aspect.Audited;
import com.rayk.health.system.dto.CreateSupportTicketRequest;
import com.rayk.health.system.dto.ReplySupportTicketRequest;
import com.rayk.health.system.entity.SupportTicketEntity;
import com.rayk.health.system.mapper.SupportTicketMapper;
import com.rayk.health.system.vo.PlatformSupportTicketVo;
import com.rayk.health.system.vo.SupportTicketVo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import com.rayk.health.common.exception.BusinessException;
import com.rayk.health.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class SupportTicketService {
    private final SupportTicketMapper ticketMapper;

    public SupportTicketService(SupportTicketMapper ticketMapper) {
        this.ticketMapper = ticketMapper;
    }

    public List<SupportTicketVo> listMine() {
        CurrentPrincipal current = CurrentUser.require();
        return ticketMapper
                .selectList(
                        new LambdaQueryWrapper<SupportTicketEntity>()
                                .eq(SupportTicketEntity::getTenantId, current.tenantId())
                                .eq(SupportTicketEntity::getUserId, current.userId())
                                .eq(SupportTicketEntity::getStatus, "RESOLVED")
                                .eq(SupportTicketEntity::getDeleted, 0)
                                .orderByDesc(SupportTicketEntity::getCreatedAt))
                .stream()
                .filter(ticket -> "RESOLVED".equals(ticket.getStatus()))
                .filter(ticket -> ticket.getReply() != null && !ticket.getReply().isBlank())
                .map(this::toVo)
                .toList();
    }

    @Audited(operationType = "CREATE_SUPPORT_TICKET", resourceType = "SUPPORT_TICKET")
    public SupportTicketVo create(CreateSupportTicketRequest request) {
        CurrentPrincipal current = CurrentUser.require();
        if (current.roles().contains("PLATFORM_ADMIN")) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        LocalDateTime now = LocalDateTime.now();

        SupportTicketEntity entity = new SupportTicketEntity();
        entity.setTenantId(current.tenantId());
        entity.setUserId(current.userId());
        entity.setCategory(request.category().trim());
        entity.setContent(request.content().trim());
        entity.setContact(normalize(request.contact()));
        entity.setStatus("OPEN");
        entity.setCreatedBy(current.userId());
        entity.setCreatedAt(now);
        entity.setUpdatedBy(current.userId());
        entity.setUpdatedAt(now);
        entity.setDeleted(0);
        entity.setVersion(0);
        ticketMapper.insert(entity);
        return toVo(entity);
    }

    public List<PlatformSupportTicketVo> listForPlatform() {
        requirePlatformAdmin();
        return ticketMapper.selectPlatformTickets(200).stream().map(this::toPlatformVo).toList();
    }

    @Audited(operationType = "REPLY_SUPPORT_TICKET", resourceType = "SUPPORT_TICKET")
    public PlatformSupportTicketVo replyForPlatform(long id, ReplySupportTicketRequest request) {
        CurrentPrincipal current = requirePlatformAdmin();
        SupportTicketEntity ticket = ticketMapper.selectPlatformTicketById(id);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        String status = normalizeReplyStatus(request.status());
        LocalDateTime now = LocalDateTime.now();
        int updated =
                ticketMapper.updatePlatformReply(
                        id, request.reply().trim(), status, current.userId(), now);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        ticket.setReply(request.reply().trim());
        ticket.setStatus(status);
        ticket.setUpdatedBy(current.userId());
        ticket.setUpdatedAt(now);
        return toPlatformVo(ticket);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeReplyStatus(String value) {
        String status = value == null || value.isBlank() ? "RESOLVED" : value.trim();
        if (!Set.of("PROCESSING", "RESOLVED", "CLOSED").contains(status)) {
            throw new BusinessException(ErrorCode.SYSTEM_VALIDATION_ERROR);
        }
        return status;
    }

    private static CurrentPrincipal requirePlatformAdmin() {
        CurrentPrincipal current = CurrentUser.require();
        if (!current.roles().contains("PLATFORM_ADMIN")) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
        }
        return current;
    }

    private SupportTicketVo toVo(SupportTicketEntity entity) {
        return new SupportTicketVo(
                String.valueOf(entity.getId()),
                entity.getCategory(),
                entity.getContent(),
                entity.getContact(),
                entity.getStatus(),
                entity.getReply(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private PlatformSupportTicketVo toPlatformVo(SupportTicketEntity entity) {
        return new PlatformSupportTicketVo(
                String.valueOf(entity.getId()),
                String.valueOf(entity.getTenantId()),
                String.valueOf(entity.getUserId()),
                entity.getCategory(),
                entity.getContent(),
                entity.getContact(),
                entity.getStatus(),
                entity.getReply(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
