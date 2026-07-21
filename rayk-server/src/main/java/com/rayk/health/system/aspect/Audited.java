package com.rayk.health.system.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在方法上，由 {@link AuditLogAspect} 自动记录操作审计日志。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    /** 操作类型，如 CREATE_PATIENT、UPDATE_PROFILE */
    String operationType();

    /** 资源类型，如 PATIENT、HEALTH_PROFILE */
    String resourceType();
}
