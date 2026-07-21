package com.rayk.health.system.aspect;

import com.rayk.health.system.application.AuditService;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP 切面：拦截 {@link Audited} 注解方法，自动记录操作审计日志。
 */
@Aspect
@Component
public class AuditLogAspect {
    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final AuditService auditService;

    public AuditLogAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String resourceId = extractResourceId(joinPoint);
        try {
            Object result = joinPoint.proceed();
            if (resourceId == null) {
                resourceId = extractResultId(result);
            }
            auditService.log(
                    audited.operationType(),
                    audited.resourceType(),
                    resourceId,
                    "SUCCESS",
                    buildDetail(joinPoint, null));
            return result;
        } catch (Throwable ex) {
            auditService.log(
                    audited.operationType(),
                    audited.resourceType(),
                    resourceId,
                    "FAILURE",
                    buildDetail(joinPoint, ex));
            throw ex;
        }
    }

    /**
     * 尝试从方法参数中提取资源 ID（取第一个参数作为 resourceId）。
     */
    private String extractResourceId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args != null
                && args.length > 0
                && (args[0] instanceof Number || args[0] instanceof CharSequence)) {
            return String.valueOf(args[0]);
        }
        return null;
    }

    private String extractResultId(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Method idAccessor = result.getClass().getMethod("id");
            Object id = idAccessor.invoke(result);
            return id == null ? null : String.valueOf(id);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private String buildDetail(ProceedingJoinPoint joinPoint, Throwable ex) {
        String method = joinPoint.getSignature().toShortString();
        if (ex != null) {
            return method + " | error: " + ex.getMessage();
        }
        return method;
    }
}
