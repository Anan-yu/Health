package com.rayk.health.system.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.system.aspect.Audited;
import com.rayk.health.system.dto.CreateSupportTicketRequest;
import com.rayk.health.system.entity.SupportTicketEntity;
import com.rayk.health.system.mapper.SupportTicketMapper;
import com.rayk.health.system.vo.SupportTicketVo;
import java.time.LocalDateTime;
import java.util.List;
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
                                .eq(SupportTicketEntity::getDeleted, 0)
                                .orderByDesc(SupportTicketEntity::getCreatedAt))
                .stream()
                .map(this::toVo)
                .toList();
    }

    @Audited(operationType = "CREATE_SUPPORT_TICKET", resourceType = "SUPPORT_TICKET")
    public SupportTicketVo create(CreateSupportTicketRequest request) {
        CurrentPrincipal current = CurrentUser.require();
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

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
}
