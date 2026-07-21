package com.rayk.health.platform.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.rayk.health.platform.vo.TenantSummaryVo;
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
    @Select("SELECT COUNT(*) FROM sys_user WHERE tenant_id <> 1 AND deleted = 0")
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
    @Select(
            """
            SELECT CAST(t.tenant_id AS CHAR) AS id,
                   t.tenant_code AS code,
                   t.tenant_name AS name,
                   t.status AS status,
                   t.service_plan AS servicePlan,
                   COUNT(u.id) AS userCount
            FROM sys_tenant t
            LEFT JOIN sys_user u ON u.tenant_id = t.tenant_id AND u.deleted = 0
            WHERE t.tenant_id <> 1 AND t.deleted = 0
            GROUP BY t.tenant_id, t.tenant_code, t.tenant_name, t.status, t.service_plan
            ORDER BY t.tenant_id
            """)
    List<TenantSummaryVo> selectTenants();
}
