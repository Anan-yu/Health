package com.rayk.health.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.system.entity.SysRoleEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;

public interface SysRoleMapper extends BaseMapper<SysRoleEntity> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_role WHERE tenant_id = #{tenantId} AND role_code = #{roleCode} AND deleted = 0 AND status = 'ACTIVE' LIMIT 1")
    SysRoleEntity selectByTenantAndCodeIgnoringTenant(
            @Param("tenantId") long tenantId, @Param("roleCode") String roleCode);
}
