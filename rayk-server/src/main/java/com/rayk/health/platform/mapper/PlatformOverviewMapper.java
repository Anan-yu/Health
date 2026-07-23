package com.rayk.health.platform.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.rayk.health.platform.vo.TenantSummaryVo;
import com.rayk.health.platform.vo.PlatformFollowupVo;
import java.util.List;
import org.apache.ibatis.annotations.Select;

public interface PlatformOverviewMapper {
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM sys_tenant WHERE tenant_id <> 1 AND deleted = 0")
    long countTenants();

    @InterceptorIgnore(tenantLine = "true")
    @Select(
            "SELECT COUNT(*) FROM sys_tenant WHERE tenant_id <> 1 AND status = 'ACTIVE' AND deleted = 0")
    long countActiveTenants();

    @InterceptorIgnore(tenantLine = "true")
    @Select(
            """
            SELECT COUNT(DISTINCT u.id)
            FROM sys_user u
            INNER JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
            INNER JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
            WHERE u.tenant_id <> 1 AND u.deleted = 0
              AND r.role_code IN ('DOCTOR')
            """)
    long countUsers();

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM health_patient WHERE deleted = 0")
    long countPatients();

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM assessment_review WHERE status = 'WAITING_REVIEW' AND deleted = 0")
    long countPendingReviews();

    @InterceptorIgnore(tenantLine = "true")
    @Select(
            "SELECT COUNT(*) FROM followup_task WHERE status NOT IN ('COMPLETED', 'CANCELLED') AND deleted = 0")
    long countPendingFollowups();

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM followup_task WHERE DATE(created_at) = CURRENT_DATE AND deleted = 0")
    long countTodayFollowups();

    @InterceptorIgnore(tenantLine = "true")
    @Select(
            """
            SELECT CAST(t.tenant_id AS CHAR) AS id,
                   t.tenant_code AS code,
                   t.tenant_name AS name,
                   t.status AS status,
                   t.service_plan AS servicePlan,
                   COUNT(DISTINCT CASE WHEN r.id IS NOT NULL THEN u.id END) AS userCount
            FROM sys_tenant t
            LEFT JOIN sys_user u ON u.tenant_id = t.tenant_id AND u.deleted = 0
            LEFT JOIN sys_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
            LEFT JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
                AND r.role_code IN ('DOCTOR')
            WHERE t.tenant_id <> 1 AND t.deleted = 0
            GROUP BY t.tenant_id, t.tenant_code, t.tenant_name, t.status, t.service_plan
            ORDER BY t.tenant_id
            """)
    List<TenantSummaryVo> selectTenants();

    @InterceptorIgnore(tenantLine = "true")
    @Select(
            """
            SELECT CAST(f.id AS CHAR) AS id,
                   p.name AS patientName,
                   f.title AS title,
                   f.due_date AS dueDate,
                   f.status AS status,
                   f.feedback AS feedback,
                   f.completed_at AS completedAt
            FROM followup_task f
            INNER JOIN health_patient p ON p.id = f.patient_id AND p.deleted = 0
            WHERE f.deleted = 0
            ORDER BY f.updated_at DESC
            LIMIT 8
            """)
    List<PlatformFollowupVo> selectRecentFollowups();
}
