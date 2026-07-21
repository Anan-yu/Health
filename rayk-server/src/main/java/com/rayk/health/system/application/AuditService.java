package com.rayk.health.system.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rayk.health.security.service.CurrentPrincipal;
import com.rayk.health.security.service.CurrentUser;
import com.rayk.health.system.entity.OperationAuditLogEntity;
import com.rayk.health.system.mapper.OperationAuditLogMapper;
import com.rayk.health.tenant.TenantContext;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuditService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{4})\\d{10}(\\d{4})");

    private final OperationAuditLogMapper auditLogMapper;

    public AuditService(OperationAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 记录操作审计日志。
     *
     * @param operationType 操作类型
     * @param resourceType  资源类型
     * @param resourceId    资源标识
     * @param result        操作结果（SUCCESS / FAILURE）
     * @param detail        操作详情（将自动脱敏）
     */
    public void log(String operationType, String resourceType, String resourceId,
                    String result, String detail) {
        long operatorId = resolveOperatorId();
        long tenantId = resolveTenantId();
        String requestId = resolveRequestId();

        OperationAuditLogEntity entity = new OperationAuditLogEntity();
        entity.setTenantId(tenantId);
        entity.setOperatorId(operatorId);
        entity.setOperationType(operationType);
        entity.setResourceType(resourceType);
        entity.setResourceId(resourceId);
        entity.setRequestId(requestId);
        entity.setResult(result);
        entity.setDetailMasked(maskSensitiveData(detail));
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setDeleted(0);
        entity.setVersion(0);
        auditLogMapper.insert(entity);
    }

    /**
     * 分页查询审计日志。
     */
    public Page<OperationAuditLogEntity> queryLogs(AuditQueryRequest request) {
        CurrentPrincipal current = CurrentUser.require();
        long pageNumber = Math.max(1, request.page());
        long pageSize = Math.min(100, Math.max(1, request.size()));
        if (current.roles().contains("PLATFORM_ADMIN")) {
            long total =
                    auditLogMapper.countPlatformLogs(
                            request.tenantId(),
                            request.operationType(),
                            request.resourceType(),
                            request.startDate(),
                            request.endDate());
            Page<OperationAuditLogEntity> page = new Page<>(pageNumber, pageSize, total);
            page.setRecords(
                    auditLogMapper.selectPlatformLogs(
                            request.tenantId(),
                            request.operationType(),
                            request.resourceType(),
                            request.startDate(),
                            request.endDate(),
                            (pageNumber - 1) * pageSize,
                            pageSize));
            return page;
        }
        LambdaQueryWrapper<OperationAuditLogEntity> query =
                new LambdaQueryWrapper<OperationAuditLogEntity>()
                        .eq(OperationAuditLogEntity::getTenantId, current.tenantId())
                        .eq(OperationAuditLogEntity::getDeleted, 0);

        if (StringUtils.hasText(request.operationType())) {
            query.eq(OperationAuditLogEntity::getOperationType, request.operationType());
        }
        if (StringUtils.hasText(request.resourceType())) {
            query.eq(OperationAuditLogEntity::getResourceType, request.resourceType());
        }
        if (request.startDate() != null) {
            query.ge(OperationAuditLogEntity::getCreatedAt, request.startDate());
        }
        if (request.endDate() != null) {
            query.le(OperationAuditLogEntity::getCreatedAt, request.endDate());
        }
        query.orderByDesc(OperationAuditLogEntity::getCreatedAt);

        Page<OperationAuditLogEntity> page = new Page<>(pageNumber, pageSize);
        return auditLogMapper.selectPage(page, query);
    }

    /**
     * 脱敏处理：手机号中间四位、身份证号中间十位。
     */
    String maskSensitiveData(String detail) {
        if (!StringUtils.hasText(detail)) {
            return detail;
        }
        String masked = PHONE_PATTERN.matcher(detail).replaceAll("$1****$2");
        masked = ID_CARD_PATTERN.matcher(masked).replaceAll("$1**********$2");
        return masked;
    }

    private long resolveOperatorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentPrincipal principal) {
            return principal.userId();
        }
        return 0L;
    }

    private long resolveTenantId() {
        Long tenantFromContext = TenantContext.get();
        if (tenantFromContext != null) {
            return tenantFromContext;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentPrincipal principal) {
            return principal.tenantId();
        }
        return 0L;
    }

    private String resolveRequestId() {
        String requestId = MDC.get("requestId");
        return StringUtils.hasText(requestId) ? requestId : UUID.randomUUID().toString();
    }

    public record AuditQueryRequest(
            Long tenantId,
            String operationType,
            String resourceType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            long page,
            long size) {}
}
