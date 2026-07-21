package com.rayk.health.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.system.entity.OperationAuditLogEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface OperationAuditLogMapper extends BaseMapper<OperationAuditLogEntity> {
    @InterceptorIgnore(tenantLine = "true")
    @Select(
            """
            <script>
            SELECT * FROM operation_audit_log
            WHERE deleted = 0
            <if test="tenantId != null">AND tenant_id = #{tenantId}</if>
            <if test="operationType != null and operationType != ''">AND operation_type = #{operationType}</if>
            <if test="resourceType != null and resourceType != ''">AND resource_type = #{resourceType}</if>
            <if test="startDate != null">AND created_at &gt;= #{startDate}</if>
            <if test="endDate != null">AND created_at &lt;= #{endDate}</if>
            ORDER BY created_at DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<OperationAuditLogEntity> selectPlatformLogs(
            @Param("tenantId") Long tenantId,
            @Param("operationType") String operationType,
            @Param("resourceType") String resourceType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("offset") long offset,
            @Param("size") long size);

    @InterceptorIgnore(tenantLine = "true")
    @Select(
            """
            <script>
            SELECT COUNT(*) FROM operation_audit_log
            WHERE deleted = 0
            <if test="tenantId != null">AND tenant_id = #{tenantId}</if>
            <if test="operationType != null and operationType != ''">AND operation_type = #{operationType}</if>
            <if test="resourceType != null and resourceType != ''">AND resource_type = #{resourceType}</if>
            <if test="startDate != null">AND created_at &gt;= #{startDate}</if>
            <if test="endDate != null">AND created_at &lt;= #{endDate}</if>
            </script>
            """)
    long countPlatformLogs(
            @Param("tenantId") Long tenantId,
            @Param("operationType") String operationType,
            @Param("resourceType") String resourceType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
