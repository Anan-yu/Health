package com.rayk.health.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rayk.health.system.entity.SysRolePermissionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysRolePermissionMapper extends BaseMapper<SysRolePermissionEntity> {
    /**
     * 查询用户经由其角色聚合得到的有效权限编码（去重）。
     */
    @Select(
            """
            SELECT DISTINCT p.permission_code
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.status = 'ACTIVE'
            JOIN sys_role_permission rp ON rp.role_id = ur.role_id AND rp.deleted = 0
            JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = 0
            WHERE ur.user_id = #{userId} AND ur.deleted = 0
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") long userId);
}
