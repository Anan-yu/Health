package com.rayk.health.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.rayk.health.system.entity.SysTenantEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SysTenantMapper extends BaseMapper<SysTenantEntity> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_tenant WHERE tenant_id = #{tenantId} AND deleted = 0 AND status = 'ACTIVE' LIMIT 1")
    SysTenantEntity selectActiveByTenantIdIgnoringTenant(@Param("tenantId") long tenantId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_tenant WHERE tenant_id = #{tenantId} AND deleted = 0 LIMIT 1")
    SysTenantEntity selectByTenantIdIgnoringTenant(@Param("tenantId") long tenantId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_tenant WHERE tenant_code = #{tenantCode} AND deleted = 0 LIMIT 1")
    SysTenantEntity selectByTenantCodeIgnoringTenant(@Param("tenantCode") String tenantCode);

    @InterceptorIgnore(tenantLine = "true")
    @Update(
            "UPDATE sys_tenant SET tenant_name = #{tenant.tenantName}, service_plan = #{tenant.servicePlan}, "
                    + "status = #{tenant.status}, updated_by = #{tenant.updatedBy}, updated_at = #{tenant.updatedAt}, "
                    + "version = version + 1 WHERE tenant_id = #{tenant.tenantId} AND deleted = 0")
    int updateProfileIgnoringTenant(@Param("tenant") SysTenantEntity tenant);
}
