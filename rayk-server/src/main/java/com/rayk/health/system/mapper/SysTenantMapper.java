package com.rayk.health.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.rayk.health.system.entity.SysTenantEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysTenantMapper extends BaseMapper<SysTenantEntity> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_tenant WHERE tenant_id = #{tenantId} AND deleted = 0 AND status = 'ACTIVE' LIMIT 1")
    SysTenantEntity selectActiveByTenantIdIgnoringTenant(@Param("tenantId") long tenantId);
}
